package com.pistolwhip.hotspot

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.IBinder
import java.io.File
import java.io.FileOutputStream
import hev.htproxy.TProxyService

/** Receiver-side device-wide tunnel using Android VpnService and a TCP/UDP SOCKS5 transport. */
class TunnelVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var tunnelStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val gateway = intent?.getStringExtra(EXTRA_GATEWAY)
        val network = readUnderlyingNetwork(intent)
        if (gateway.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startTunnel(gateway, network)
        return START_STICKY
    }

    @Suppress("DEPRECATION")
    private fun readUnderlyingNetwork(intent: Intent?): Network? {
        return if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_NETWORK, Network::class.java)
        else intent?.getParcelableExtra(EXTRA_NETWORK)
    }

    private fun startTunnel(gateway: String, network: Network?) {
        if (tun != null) return
        val notification = Notifications.build(this, "Receive VPN", "Starting device-wide tunnel")
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)

        try {
            if (Build.VERSION.SDK_INT >= 22 && network != null) setUnderlyingNetworks(arrayOf(network))
            val builder = Builder()
                .setSession("Hotspot Pistolwhip'd")
                .setBlocking(false)
                .setMtu(TUN_MTU)
                .addAddress(TUN_IPV4, 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(TUN_DNS)
                .addAddress(TUN_IPV6, 126)
                .addRoute("::", 0)
                .addDnsServer(TUN_IPV6_DNS)
            // Exclude this app's UID so the native SOCKS transport cannot loop into TUN.
            builder.addDisallowedApplication(packageName)
            tun = builder.establish()
            val descriptor = tun ?: throw IllegalStateException("Android did not establish the VPN interface")

            val config = File(cacheDir, "receiver-tunnel.yml")
            FileOutputStream(config, false).use { stream ->
                val yaml = """
                    tunnel:
                      name: tun0
                      mtu: $TUN_MTU
                      ipv4: $TUN_IPV4
                      ipv6: '$TUN_IPV6'
                      icmp: 'reply'
                    socks5:
                      address: '$gateway'
                      port: ${ShareService.PORT}
                      udp: 'udp'
                    mapdns:
                      address: $TUN_DNS
                      port: 53
                      network: 240.0.0.0
                      netmask: 240.0.0.0
                      cache-size: 10000
                    misc:
                      max-session-count: 2048
                      connect-timeout: 12000
                      tcp-read-write-timeout: 300000
                      udp-read-write-timeout: 60000
                      log-level: error
                """.trimIndent()
                stream.write(yaml.toByteArray(Charsets.UTF_8))
            }

            tunnelStarted = TProxyService.TProxyStartService(config.absolutePath, descriptor.fd)
            if (!tunnelStarted || !TProxyService.TProxyIsRunning()) throw IllegalStateException("Native TCP/UDP tunnel did not start")
            State.setTunnel(this, true)
            (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .notify(NOTIFICATION_ID, Notifications.build(this, "Receive VPN", "Device traffic is routed through the sharing phone"))
        } catch (error: Throwable) {
            stopTunnel()
            (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .notify(NOTIFICATION_ID, Notifications.build(this, "Receive VPN unavailable", error.message ?: "Could not start VPN tunnel"))
            stopSelf()
        }
    }

    private fun stopTunnel() {
        try { if (tunnelStarted) TProxyService.TProxyStopService() } catch (_: Throwable) { }
        tunnelStarted = false
        State.setTunnel(this, false)
        try { tun?.close() } catch (_: Exception) { }
        tun = null
    }

    override fun onRevoke() {
        stopTunnel()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    companion object {
        const val ACTION_START = "com.pistolwhip.hotspot.vpn.START"
        const val ACTION_STOP = "com.pistolwhip.hotspot.vpn.STOP"
        const val EXTRA_GATEWAY = "gateway"
        const val EXTRA_NETWORK = "underlying_network"
        const val NOTIFICATION_ID = 13
        private const val TUN_MTU = 1500
        private const val TUN_IPV4 = "198.18.0.1"
        private const val TUN_DNS = "198.18.0.2"
        private const val TUN_IPV6 = "fc00::1"
        private const val TUN_IPV6_DNS = "2001:4860:4860::8888"

        fun start(context: Context, gateway: String, network: Network) {
            val intent = Intent(context, TunnelVpnService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_GATEWAY, gateway)
                .putExtra(EXTRA_NETWORK, network)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
