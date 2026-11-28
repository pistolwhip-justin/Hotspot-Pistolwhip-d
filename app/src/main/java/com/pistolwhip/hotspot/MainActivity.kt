package com.pistolwhip.hotspot

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.VpnService
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {
    private enum class PendingAction { NONE, SHARE, RECEIVE }
    private var pendingAction = PendingAction.NONE
    private var showCredentialDialog by mutableStateOf(false)
    private var credentialSsid by mutableStateOf("")
    private var credentialPassword by mutableStateOf("")
    private var awaitingWifiSettingsReturn = false
    private var vpnConsentInProgress = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val nearbyPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAction
        pendingAction = PendingAction.NONE
        if (granted) performAction(action)
        else State.setReceiveMessage("Nearby Wi-Fi permission was denied")
    }

    private val vpnConsentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vpnConsentInProgress = false
        if (result.resultCode == RESULT_OK) openReceiveFlow()
        else {
            startService(Intent(this, ReceiveService::class.java).setAction(ReceiveService.ACTION_STOP))
            State.setReceive(this, false)
            State.setReceiveMessage("Receive needs Android VPN permission")
        }
    }

    private val qrScanner = registerForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw != null) {
            val credentials = WifiCredentialRules.parseWifiQr(raw)
            if (credentials != null) {
                credentialSsid = credentials.ssid
                credentialPassword = credentials.passphrase
                showCredentialDialog = true
            } else {
                State.setReceiveMessage("QR code is not a valid DIRECT-WHIP Wi-Fi credential")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 40)
        }
        setContent {
            HotspotTheme {
                Dashboard(
                    showCredentialDialog = showCredentialDialog,
                    ssid = credentialSsid,
                    password = credentialPassword,
                    onSsidChange = { credentialSsid = it },
                    onPasswordChange = { credentialPassword = it },
                    onShare = { enabled -> toggleShare(enabled) },
                    onReceive = { enabled -> toggleReceive(enabled) },
                    onConnect = { ssid, password -> connectToWifiDirect(ssid, password) },
                    onScan = { scanWifiQr() },
                    onDismissCredentials = {
                        showCredentialDialog = false
                        State.setReceive(this, false)
                    },
                    onWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                )
            }
        }
    }

    private fun toggleShare(enabled: Boolean) {
        if (!enabled) {
            startService(Intent(this, ShareService::class.java).setAction(ShareService.ACTION_STOP))
            State.setShare(this, false)
            return
        }
        ensureNearbyPermission(PendingAction.SHARE)
    }

    private fun toggleReceive(enabled: Boolean) {
        if (!enabled) {
            startService(Intent(this, ReceiveService::class.java).setAction(ReceiveService.ACTION_STOP))
            stopService(Intent(this, TunnelVpnService::class.java))
            State.setReceive(this, false)
            return
        }
        ensureNearbyPermission(PendingAction.RECEIVE)
    }

    private fun ensureNearbyPermission(action: PendingAction) {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            performAction(action)
        } else {
            pendingAction = action
            nearbyPermissionLauncher.launch(permission)
        }
    }

    private fun performAction(action: PendingAction) {
        when (action) {
            PendingAction.SHARE -> {
                val intent = Intent(this, ShareService::class.java)
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            }
            PendingAction.RECEIVE -> requestVpnConsentOrContinue()
            PendingAction.NONE -> Unit
        }
    }

    private fun requestVpnConsentOrContinue() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnConsentInProgress = true
            vpnConsentLauncher.launch(vpnIntent)
        } else {
            openReceiveFlow()
        }
    }

    @Suppress("DEPRECATION")
    private fun openReceiveFlow() {
        State.setReceive(this, true)
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifi.isWifiEnabled) {
            if (Build.VERSION.SDK_INT < 29 && runCatching { wifi.setWifiEnabled(true) }.getOrDefault(false)) {
                State.setReceiveMessage("Wi-Fi is turning on; checking for the Share network")
                mainHandler.postDelayed({ openReceiveFlow() }, 10_000)
            } else {
                awaitingWifiSettingsReturn = true
                State.setReceiveMessage("Enable Wi-Fi in Android settings, then return to Receive")
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            return
        }
        val currentSsid = wifi.connectionInfo?.ssid.orEmpty().trim('"')
        if (currentSsid.startsWith(WifiCredentialRules.SSID_PREFIX)) {
            State.setReceive(this, true)
            State.setReceiveSsid(currentSsid)
            State.setReceiveMessage("Checking the connected Wi-Fi Direct group")
            val intent = Intent(this, ReceiveService::class.java).setAction(ReceiveService.ACTION_MONITOR)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } else {
            credentialSsid = ""
            credentialPassword = ""
            State.setReceiveMessage("Scan the Share phone's QR code or enter its Wi-Fi credentials")
            showCredentialDialog = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (State.receive(this) && !vpnConsentInProgress) {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnConsentInProgress = true
                vpnConsentLauncher.launch(vpnIntent)
            }
        }
        if (awaitingWifiSettingsReturn) {
            awaitingWifiSettingsReturn = false
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifi.isWifiEnabled) {
                mainHandler.postDelayed({ openReceiveFlow() }, 10_000)
            } else {
                State.setReceiveMessage("Wi-Fi is still off; enable it and tap Receive again")
            }
        }
    }

    private fun connectToWifiDirect(ssid: String, password: String) {
        val cleanSsid = ssid.trim()
        if (!cleanSsid.startsWith(WifiCredentialRules.SSID_PREFIX) ||
            !WifiCredentialRules.isValidPassphrase(password)
        ) {
            State.setReceiveMessage("Enter a DIRECT-WHIP- SSID and a valid Share password")
            return
        }
        showCredentialDialog = false
        State.setReceive(this, true)
        State.setReceiveSsid(cleanSsid)
        State.setReceiveMessage("Requesting Android Wi-Fi connection")
        val intent = Intent(this, ReceiveService::class.java)
            .setAction(ReceiveService.ACTION_CONNECT)
            .putExtra(ReceiveService.EXTRA_SSID, cleanSsid)
            .putExtra(ReceiveService.EXTRA_PASSWORD, password)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }

    private fun scanWifiQr() {
        qrScanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan the Share phone's Wi-Fi QR code")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    private fun serviceToggle(cls: Class<*>, stopAction: String, enabled: Boolean) {
        val intent = Intent(this, cls).apply { if (!enabled) action = stopAction }
        if (Build.VERSION.SDK_INT >= 26 && enabled) startForegroundService(intent) else startService(intent)
        if (cls == ShareService::class.java) State.setShare(this, enabled)
        else State.setReceive(this, enabled)
    }
}

@Composable
private fun HotspotTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dynamic = if (Build.VERSION.SDK_INT >= 31) androidx.compose.material3.dynamicDarkColorScheme(context) else darkColorScheme()
    val scheme = dynamic.copy(background = Color.Black, surface = Color.Black, surfaceContainer = Color(0xff111111), surfaceContainerHigh = Color(0xff191919))
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun Dashboard(
    showCredentialDialog: Boolean,
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onShare: (Boolean) -> Unit,
    onReceive: (Boolean) -> Unit,
    onConnect: (String, String) -> Unit,
    onScan: () -> Unit,
    onDismissCredentials: () -> Unit,
    onWifiSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(context) { State.refresh(context) }
    val sharing by State.shareActive.collectAsState()
    val receiving by State.receiveActive.collectAsState()
    val shareHealthy by State.shareHealthy.collectAsState()
    val receiveHealthy by State.receiveHealthy.collectAsState()
    val shareSsid by State.shareSsid.collectAsState()
    val sharePassphrase by State.sharePassphrase.collectAsState()
    val shareAddress by State.shareAddress.collectAsState()
    val receiveMessage by State.receiveMessage.collectAsState()
    val receiveSsid by State.receiveSsid.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        State.refresh(context)
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Hotspot Pistolwhip'd", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text("A direct phone-to-phone connection.", color = Color(0xffaaaaaa), fontSize = 12.sp)
            }
            RoleCard("Share", "Create a Wi-Fi Direct group and share this phone's internet connection.", sharing, shareHealthy, "Wi-Fi Direct · SOCKS5") {
                onShare(it)
            }
            if (sharing && shareSsid.isNotBlank() && sharePassphrase.isNotBlank()) {
                ShareCredentialsPanel(shareSsid, sharePassphrase, shareAddress)
            }
            RoleCard("Receive", "Join the Share phone's Wi-Fi Direct group and route device traffic through it.", receiving, receiveHealthy, "VPN · TCP + UDP") {
                onReceive(it)
            }
            if (receiving) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xff111111)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(receiveSsid.ifBlank { "Receive status" }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(receiveMessage, color = if (receiveHealthy) Color(0xff27c46b) else Color(0xffaaaaaa), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
            if (!receiving) {
                OutlinedButton(onClick = onWifiSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Open Wi-Fi settings", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("P I S T O L  W H I P  P R O D U C T I O N S", color = Color(0xff666666), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    if (showCredentialDialog) {
        CredentialDialog(
            ssid = ssid,
            password = password,
            onSsidChange = onSsidChange,
            onPasswordChange = onPasswordChange,
            onConnect = { onConnect(ssid, password) },
            onScan = onScan,
            onDismiss = onDismissCredentials
        )
    }
}

@Composable
private fun RoleCard(title: String, description: String, checked: Boolean, healthy: Boolean, detail: String, onChecked: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xff111111)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(description, color = Color(0xffaaaaaa), fontSize = 12.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.width(18.dp))
                Switch(checked = checked, onCheckedChange = onChecked)
            }
            HorizontalDivider(color = Color(0xff292929))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val color = if (checked && healthy) Color(0xff27c46b) else Color(0xffdf4747)
                val status = when {
                    !checked -> "INACTIVE"
                    healthy -> "READY"
                    else -> "WAITING"
                }
                Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50)))
                Text(status, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text(detail, color = Color(0xff888888), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ShareCredentialsPanel(ssid: String, passphrase: String, address: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showQr by remember(ssid, passphrase) { mutableStateOf(false) }
    val qrBitmap = remember(ssid, passphrase) {
        runCatching {
            val payload = "WIFI:T:WPA;S:${WifiCredentialRules.escapeWifiQr(ssid)};P:${WifiCredentialRules.escapeWifiQr(passphrase)};;"
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 480, 480)
            Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888).also { bitmap ->
                for (x in 0 until 480) for (y in 0 until 480) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }.getOrNull()
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xff111111)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Wi-Fi Direct credentials", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("SSID  $ssid", color = Color(0xffbdbdbd), fontSize = 12.sp)
            Text("PASS  $passphrase", color = Color(0xffbdbdbd), fontSize = 12.sp)
            if (address.isNotBlank()) Text("P2P IP  $address:${ShareService.PORT}", color = Color(0xff888888), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    val payload = "SSID: $ssid\nPassword: $passphrase"
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Wi-Fi credentials", payload))
                }, modifier = Modifier.weight(1f)) { Text("Copy") }
                OutlinedButton(onClick = { showQr = true }, modifier = Modifier.weight(1f)) { Text("QR code") }
                Button(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "SSID: $ssid\nPassword: $passphrase")
                    }
                    context.startActivity(Intent.createChooser(send, "Share Wi-Fi credentials"))
                }, modifier = Modifier.weight(1f)) { Text("Share") }
            }
        }
    }
    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("Scan to join Wi-Fi") },
            text = {
                if (qrBitmap != null) Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "Wi-Fi network QR code", modifier = Modifier.fillMaxWidth())
                else Text("QR code could not be generated.")
            },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun CredentialDialog(
    ssid: String,
    password: String,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConnect: () -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to Share") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scan the Share phone's Wi-Fi QR code or enter the credentials it displays.", fontSize = 12.sp)
                OutlinedTextField(value = ssid, onValueChange = onSsidChange, label = { Text("Wi-Fi name (SSID)") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Password") }, singleLine = true)
                OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan QR code") }
            }
        },
        confirmButton = {
            Button(
                onClick = onConnect,
                enabled = ssid.startsWith(WifiCredentialRules.SSID_PREFIX) && WifiCredentialRules.isValidPassphrase(password)
            ) { Text("Connect") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
