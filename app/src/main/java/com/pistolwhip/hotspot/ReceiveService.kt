package com.pistolwhip.hotspot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.IBinder
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import hev.htproxy.TProxyService

class ReceiveService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var currentNetwork: Network? = null
    @Volatile private var monitoring = false
    @Volatile private var activeGateway: String? = null
    @Volatile private var activeNetwork: Network? = null
    @Volatile private var tunnelRequestedAt = 0L

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            currentNetwork = network
            scheduleCheck()
        }
        override fun onLost(network: Network) {
            if (currentNetwork == network) currentNetwork = null
            stopTunnel()
            update(false, "Wi-Fi disconnected — join the sharing phone's hotspot")
        }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
            currentNetwork = network
            scheduleCheck()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTunnel()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(12, Notifications.build(this, "Receiving link", "Checking Wi-Fi and VPN authorization"))
        if (VpnService.prepare(this) != null) {
            State.setReceive(this, false)
            update(false, "Open the app and tap Receive to approve Android's VPN permission")
            // Keep the notification available; its tap opens the app. The tile itself
            // never launches or foregrounds the Activity.
            return START_STICKY
        }

        State.setReceive(this, true)
        if (!monitoring) {
            monitoring = true
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try {
                cm.registerNetworkCallback(
                    NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                    callback
                )
            } catch (_: Exception) { }
            currentNetwork = cm.allNetworks.firstOrNull {
                cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
            executor.scheduleWithFixedDelay({ checkLink() }, 0, 15, TimeUnit.SECONDS)
        } else {
            scheduleCheck()
        }
        return START_STICKY
    }

    private fun scheduleCheck() {
        if (monitoring) executor.execute { checkLink() }
    }

    private fun checkLink() {
        if (VpnService.prepare(this) != null) {
            stopTunnel()
            State.setReceive(this, false)
            update(false, "VPN permission is required — open the app and tap Receive")
            return
        }

        val network = currentNetwork ?: run {
            stopTunnel()
            update(false, "Connect to the sharing phone's Wi-Fi")
            return
        }
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
            stopTunnel()
            update(false, "Waiting for the sharing phone's Wi-Fi")
            return
        }
        val gateway = cm.getLinkProperties(network)?.routes
            ?.firstOrNull { it.destination.prefixLength == 0 && it.gateway != null }?.gateway?.hostAddress
            ?: run {
                stopTunnel()
                update(false, "Could not identify the Wi-Fi gateway")
                return
            }

        if (!probeSocksUpstream(network, gateway)) {
            stopTunnel()
            update(false, "Sharing endpoint or upstream internet is unavailable")
            return
        }

        if (activeGateway != gateway || activeNetwork != network) {
            stopTunnel()
            activeGateway = gateway
            activeNetwork = network
            tunnelRequestedAt = System.currentTimeMillis()
            State.setTunnel(this, false)
            TunnelVpnService.start(this, gateway, network)
            update(false, "Sharing endpoint verified — starting device-wide VPN")
            return
        }

        if (State.tunnel(this) && nativeTunnelRunning()) {
            update(true, "VPN active · sharing endpoint and upstream SOCKS connection verified")
        } else if (State.tunnel(this)) {
            stopTunnel()
            update(false, "VPN engine stopped — restarting the tunnel")
        } else if (System.currentTimeMillis() - tunnelRequestedAt > 15_000) {
            update(false, "VPN tunnel did not become active — check the Receive VPN notification")
        } else {
            update(false, "Sharing endpoint verified — starting device-wide VPN")
        }
    }

    private fun nativeTunnelRunning(): Boolean {
        return try { TProxyService.TProxyIsRunning() } catch (_: Throwable) { false }
    }

    /** Verify a real SOCKS5 CONNECT to an Internet host over the selected Wi-Fi network. */
    private fun probeSocksUpstream(network: Network, gateway: String): Boolean {
        return try {
            network.socketFactory.createSocket().use { socket ->
                socket.connect(InetSocketAddress(gateway, ShareService.PORT), 3500)
                socket.soTimeout = 3500
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                output.write(byteArrayOf(5, 1, 0))
                output.flush()
                val greeting = readExact(input, 2)
                if (greeting[0].toInt() != 5 || greeting[1].toInt() != 0) return false

                val host = "example.com".toByteArray(Charsets.US_ASCII)
                val request = ByteArrayOutputStream().apply {
                    write(byteArrayOf(5, 1, 0, 3, host.size.toByte()))
                    write(host)
                    write(byteArrayOf(1, 0xbb.toByte())) // TCP port 443
                }.toByteArray()
                output.write(request)
                output.flush()
                val response = readExact(input, 4)
                response[0].toInt() == 5 && response[1].toInt() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readExact(input: java.io.InputStream, count: Int): ByteArray {
        val bytes = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(bytes, offset, count - offset)
            if (read < 0) throw java.io.EOFException()
            offset += read
        }
        return bytes
    }

    private fun stopTunnel() {
        try { stopService(Intent(this, TunnelVpnService::class.java)) } catch (_: Exception) { }
        State.setTunnel(this, false)
        activeGateway = null
        activeNetwork = null
        tunnelRequestedAt = 0L
    }

    private fun update(healthy: Boolean, message: String) {
        State.setReceiveHealthy(this, healthy)
        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(12, Notifications.build(this, "Receiving link", message, healthy))
    }

    override fun onDestroy() {
        try {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(callback)
        } catch (_: Exception) { }
        stopTunnel()
        monitoring = false
        executor.shutdownNow()
        State.setReceive(this, false)
        State.setReceiveHealthy(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.pistolwhip.hotspot.STOP_RECEIVE"
    }
}
