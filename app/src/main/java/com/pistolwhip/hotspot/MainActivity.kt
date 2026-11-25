package com.pistolwhip.hotspot

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        setContent {
            HotspotTheme {
                Dashboard(
                    initialShare = State.share(this),
                    initialReceive = State.receive(this),
                    onShare = { enabled -> serviceToggle(ShareService::class.java, ShareService.ACTION_STOP, enabled) },
                    onReceive = { enabled -> receiveToggle(enabled) },
                    onWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                )
            }
        }
    }

    private fun receiveToggle(enabled: Boolean) {
        if (!enabled) {
            serviceToggle(ReceiveService::class.java, ReceiveService.ACTION_STOP, false)
            stopService(Intent(this, TunnelVpnService::class.java))
            return
        }
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            State.setReceive(this, false)
            startActivityForResult(permissionIntent, REQUEST_VPN_PERMISSION)
        } else {
            serviceToggle(ReceiveService::class.java, ReceiveService.ACTION_STOP, true)
        }
    }

    @Deprecated("Activity result is used for Android's system VPN-consent screen")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == RESULT_OK) {
                serviceToggle(ReceiveService::class.java, ReceiveService.ACTION_STOP, true)
            } else {
                State.setReceive(this, false)
            }
        }
    }

    private fun serviceToggle(cls: Class<*>, stopAction: String, enabled: Boolean) {
        val intent = Intent(this, cls).apply { if (!enabled) action = stopAction }
        if (Build.VERSION.SDK_INT >= 26 && enabled) startForegroundService(intent) else startService(intent)
        if (cls == ShareService::class.java) State.setShare(this, enabled) else State.setReceive(this, enabled)
    }

    companion object {
        private const val REQUEST_VPN_PERMISSION = 42
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
private fun Dashboard(initialShare: Boolean, initialReceive: Boolean, onShare: (Boolean) -> Unit, onReceive: (Boolean) -> Unit, onWifiSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sharing by remember { mutableStateOf(initialShare) }
    var receiving by remember { mutableStateOf(initialReceive) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        sharing = State.share(context)
        receiving = State.receive(context)
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Hotspot Pistolwhip'd", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                Text("Share your connection. Join from another phone.", color = Color(0xffaaaaaa), fontSize = 12.sp)
            }
            RoleCard("Share", "Make this phone's hotspot connection available to other devices.", sharing, "SOCKS5 · Port 1080") { value ->
                sharing = value
                onShare(value)
            }
            RoleCard("Receive", "Connect to the sharing phone's Wi-Fi network first, then activate the tunnel.", receiving, "VPN · TCP + UDP") { value ->
                receiving = value
                onReceive(value)
            }
            if (!receiving) {
                OutlinedButton(onClick = onWifiSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Open Wi-Fi settings", fontSize = 13.sp)
                }
            } else {
                Text(
                    "Receiver traffic is routed through the sharing phone. Android's VPN notification shows tunnel status.",
                    color = Color(0xff999999), fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("P I S T O L  W H I P  P R O D U C T I O N S", color = Color(0xff666666), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun RoleCard(title: String, description: String, checked: Boolean, detail: String, onChecked: (Boolean) -> Unit) {
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
                val color = if (checked) Color(0xff27c46b) else Color(0xffdf4747)
                Box(Modifier.size(7.dp).background(color, RoundedCornerShape(50)))
                Text(if (checked) "ACTIVE" else "INACTIVE", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text(detail, color = Color(0xff888888), fontSize = 10.sp)
            }
        }
    }
}
