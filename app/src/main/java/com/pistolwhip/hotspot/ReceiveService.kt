package com.pistolwhip.hotspot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.net.wifi.WifiInfo
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import hev.htproxy.TProxyService

/** Receiver-side network association, endpoint validation and VPN lifecycle. */
class ReceiveService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var currentNetwork: Network? = null
    @Volatile private var currentSsid: String? = null
    @Volatile private var requestedNetworkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var monitorCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var connectivity: ConnectivityManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopReceiver()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(12, Notifications.build(this, "Receiving link", "Preparing Wi-Fi Direct connection"))
        connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        when (intent?.action) {
            ACTION_CONNECT -> {
                val ssid = intent.getStringExtra(EXTRA_SSID).orEmpty()
                val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
                requestSpecifiedNetwork(ssid, password)
            }
            ACTION_MONITOR -> monitorExistingNetwork()
            else -> {
                update(false, "Open the app and choose a Wi-Fi Direct network")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun requestSpecifiedNetwork(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT < 29) {
            update(false, "App-assisted Wi-Fi connection requires Android 10 or newer; use Android Wi-Fi settings")
            stopSelf()
            return
        }
        if (!ssid.startsWith(WifiCredentialRules.SSID_PREFIX) || !WifiCredentialRules.isValidPassphrase(password)) {
            update(false, "The Wi-Fi Direct network name or password is invalid")
            stopSelf()
            return
        }

        val cm = connectivity ?: return
        releaseNetworkCallbacks()
        currentSsid = ssid
        State.setReceive(this, true)
        State.setReceiveSsid(ssid)
        update(false, "Requesting Android to connect to $ssid")

        try {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    currentNetwork = network
                    executor.execute { verifyNetworkAndStartTunnel(network, ssid) }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (network == currentNetwork) executor.execute { verifyNetworkAndStartTunnel(network, ssid) }
                }

                override fun onLost(network: Network) {
                    if (network == currentNetwork) {
                        currentNetwork = null
                        stopTunnel()
                        update(false, "Wi-Fi Direct connection lost; waiting for Android to reconnect")
                    }
                }

                override fun onUnavailable() {
                    update(false, "Android could not connect to that Wi-Fi Direct network")
                    State.setReceive(this@ReceiveService, false)
                    stopSelf()
                }
            }
            requestedNetworkCallback = callback
            cm.requestNetwork(request, callback, NETWORK_REQUEST_TIMEOUT_MS)
        } catch (error: SecurityException) {
            update(false, "Nearby Wi-Fi permission is required: " + (error.message ?: "permission denied"))
            State.setReceive(this, false)
            stopSelf()
        } catch (error: Exception) {
            update(false, error.message ?: "Could not request the Wi-Fi Direct connection")
            State.setReceive(this, false)
            stopSelf()
        }
    }

    private fun monitorExistingNetwork() {
        val cm = connectivity ?: return
        State.setReceive(this, true)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val ssid = readSsid(network) ?: return
                if (ssid.startsWith(WifiCredentialRules.SSID_PREFIX)) {
                    currentNetwork = network
                    currentSsid = ssid
                    State.setReceiveSsid(ssid)
                    executor.execute { verifyNetworkAndStartTunnel(network, ssid) }
                }
            }

            override fun onLost(network: Network) {
                if (network == currentNetwork) {
                    currentNetwork = null
                    stopTunnel()
                    update(false, "Wi-Fi Direct connection lost")
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
                if (network == currentNetwork) executor.execute { verifyNetworkAndStartTunnel(network, currentSsid.orEmpty()) }
            }
        }
        monitorCallback = callback
        try {
            cm.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                callback
            )
        } catch (_: Exception) { }

        val candidate = cm.allNetworks.firstOrNull { network ->
            cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                (readSsid(network)?.startsWith(WifiCredentialRules.SSID_PREFIX) == true)
        }
        if (candidate != null) {
            currentNetwork = candidate
            currentSsid = readSsid(candidate)
            State.setReceiveSsid(currentSsid.orEmpty())
            executor.execute { verifyNetworkAndStartTunnel(candidate, currentSsid.orEmpty()) }
        } else {
            update(false, "Join a DIRECT-WHIP- network, then activate Receive")
        }
    }

    private fun readSsid(network: Network): String? {
        val cm = connectivity ?: return null
        val info = cm.getNetworkCapabilities(network)?.transportInfo as? WifiInfo ?: return null
        val value = info.ssid.trim('"')
        return value.takeIf { it.isNotBlank() && it != WifiInfo.UNKNOWN_SSID }
    }

    private fun gatewayFor(network: Network): String? {
        val cm = connectivity ?: return null
        val linkProperties = cm.getLinkProperties(network)
        val routeGateway = linkProperties?.routes
            ?.firstOrNull { it.destination.prefixLength == 0 && it.gateway != null }?.gateway
            ?: linkProperties?.routes?.firstOrNull { it.gateway != null }?.gateway
        if (routeGateway != null && !routeGateway.isAnyLocalAddress) return routeGateway.hostAddress

        // Some OEM Wi-Fi Direct clients expose the group-owner gateway through
        // DhcpInfo rather than the Network's default-route list.
        return try {
            val dhcp = (getSystemService(Context.WIFI_SERVICE) as WifiManager).dhcpInfo
            val address = dhcp.gateway
            if (address == 0) null else
                java.net.InetAddress.getByAddress(byteArrayOf(
                    (address and 0xff).toByte(),
                    (address shr 8 and 0xff).toByte(),
                    (address shr 16 and 0xff).toByte(),
                    (address shr 24 and 0xff).toByte()
                )).hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun verifyNetworkAndStartTunnel(network: Network, ssid: String) {
        if (!State.receive(this) || network != currentNetwork) return
        if (!ssid.startsWith(WifiCredentialRules.SSID_PREFIX)) {
            update(false, "Connected network is not a DIRECT-WHIP- group")
            return
        }
        val gateway = gatewayFor(network) ?: run {
            update(false, "Android did not expose the Wi-Fi Direct group-owner address")
            return
        }
        if (!probeSocksUpstream(network, gateway)) {
            stopTunnel()
            update(false, "Wi-Fi Direct is connected, but the Share SOCKS endpoint or upstream internet is unavailable")
            return
        }

        if (VpnService.prepare(this) != null) {
            stopTunnel()
            update(false, "Open Hotspot Pistolwhip'd and approve Android's VPN permission")
            return
        }

        if (State.tunnel(this) && nativeTunnelRunning()) {
            update(true, "VPN engine active · upstream SOCKS endpoint verified")
            return
        }

        update(false, "Share endpoint verified · starting device-wide VPN")
        TunnelVpnService.start(this, gateway, network)
        repeat(24) {
            if (!State.receive(this) || network != currentNetwork) return
            if (State.tunnel(this) && nativeTunnelRunning()) {
                update(true, "VPN engine active · end-to-end traffic still requires device testing")
                return
            }
            try { Thread.sleep(500) } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
        update(false, "VPN tunnel did not become active; check the Receive VPN notification")
    }

    private fun nativeTunnelRunning(): Boolean =
        try { TProxyService.TProxyIsRunning() } catch (_: Throwable) { false }

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
                    write(byteArrayOf(1, 0xbb.toByte()))
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
            if (read < 0) throw EOFException()
            offset += read
        }
        return bytes
    }

    private fun stopTunnel() {
        try { stopService(Intent(this, TunnelVpnService::class.java)) } catch (_: Exception) { }
        State.setTunnel(this, false)
        State.setReceiveHealthy(this, false)
    }

    private fun releaseNetworkCallbacks() {
        val cm = connectivity ?: return
        try { requestedNetworkCallback?.let { cm.unregisterNetworkCallback(it) } } catch (_: Exception) { }
        try { monitorCallback?.let { cm.unregisterNetworkCallback(it) } } catch (_: Exception) { }
        requestedNetworkCallback = null
        monitorCallback = null
    }

    private fun stopReceiver() {
        releaseNetworkCallbacks()
        currentNetwork = null
        currentSsid = null
        stopTunnel()
        State.setReceive(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun update(healthy: Boolean, message: String) {
        State.setReceiveHealthy(this, healthy)
        State.setReceiveMessage(message)
        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .notify(12, Notifications.build(this, "Receiving link", message, healthy))
    }

    override fun onDestroy() {
        releaseNetworkCallbacks()
        stopTunnel()
        executor.shutdownNow()
        State.setReceive(this, false)
        State.setReceiveHealthy(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.pistolwhip.hotspot.STOP_RECEIVE"
        const val ACTION_CONNECT = "com.pistolwhip.hotspot.CONNECT_WIFI_DIRECT"
        const val ACTION_MONITOR = "com.pistolwhip.hotspot.MONITOR_WIFI_DIRECT"
        const val EXTRA_SSID = "wifi_direct_ssid"
        const val EXTRA_PASSWORD = "wifi_direct_password"
        private const val NETWORK_REQUEST_TIMEOUT_MS = 45_000
    }
}
