package com.pistolwhip.hotspot

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        setContent { HotspotTheme { Dashboard(
            initialShare = State.share(this), initialReceive = State.receive(this),
            onShare = { enabled -> serviceToggle(ShareService::class.java, ShareService.ACTION_STOP, enabled) },
            onReceive = { enabled -> serviceToggle(ReceiveService::class.java, ReceiveService.ACTION_STOP, enabled) },
            onWifiSettings = { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        ) } }
    }
    private fun serviceToggle(cls: Class<*>, stopAction: String, enabled: Boolean) {
        val intent = Intent(this, cls).apply { if (!enabled) action = stopAction }
        if (Build.VERSION.SDK_INT >= 26 && enabled) startForegroundService(intent) else startService(intent)
        if (cls == ShareService::class.java) State.setShare(this, enabled) else State.setReceive(this, enabled)
    }
}

@Composable
private fun HotspotTheme(content: @Composable () -> Unit) {
    val dynamic = if (Build.VERSION.SDK_INT >= 31) androidx.compose.material3.dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current) else darkColorScheme()
    val scheme = dynamic.copy(background = Color.Black, surface = Color.Black, surfaceContainer = Color(0xff111111), surfaceContainerHigh = Color(0xff191919))
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun Dashboard(initialShare: Boolean, initialReceive: Boolean, onShare: (Boolean) -> Unit, onReceive: (Boolean) -> Unit, onWifiSettings: () -> Unit) {
    var sharing by remember { mutableStateOf(initialShare) }
    var receiving by remember { mutableStateOf(initialReceive) }
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("HOTSPOT", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Connection", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                Text("Share your connection. Join from another phone.", color = Color(0xffaaaaaa), fontSize = 14.sp)
            }
            RoleCard("Share", "Make this phone's hotspot connection available to other devices.", sharing, "SOCKS5 · Port 1080") { value ->
                sharing = value; onShare(value)
            }
            RoleCard("Receive", "Connect to the sharing phone's Wi-Fi network first, then activate receiving.", receiving, "Wi-Fi network · Gateway discovery") { value ->
                receiving = value; onReceive(value)
            }
            if (!receiving) {
                OutlinedButton(onClick = onWifiSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text("Open Wi-Fi settings")
                }
            } else {
                Text("Phase 1 verifies the local sharing endpoint and internet reachability. Full-device VPN routing is coming in Phase 2.", color = Color(0xff999999), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("P I S T O L  W H I P  P R O D U C T I O N S", color = Color(0xff666666), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun RoleCard(title: String, description: String, checked: Boolean, detail: String, onChecked: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xff111111)), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(description, color = Color(0xffaaaaaa), fontSize = 13.sp, lineHeight = 18.sp)
                }
                Switch(checked = checked, onCheckedChange = onChecked)
            }
            HorizontalDivider(color = Color(0xff292929))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val color = if (checked) Color(0xff27c46b) else Color(0xffdf4747)
                Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
                Text(if (checked) "ACTIVE" else "INACTIVE", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text(detail, color = Color(0xff888888), fontSize = 11.sp)
            }
        }
    }
}
