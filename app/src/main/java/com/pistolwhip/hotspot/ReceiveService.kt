package com.pistolwhip.hotspot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors

class ReceiveService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var currentNetwork: Network? = null
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { currentNetwork = network; scheduleCheck() }
        override fun onLost(network: Network) { if (currentNetwork == network) currentNetwork = null; update(false, "Wi-Fi disconnected") }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) { currentNetwork = network; scheduleCheck() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(12, Notifications.build(this, "Receiving link", "Checking hotspot connection"))
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try { cm.registerNetworkCallback(NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), callback) } catch (_: Exception) { }
        currentNetwork = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        scheduleCheck()
        return START_STICKY
    }

    private fun scheduleCheck() { executor.execute { checkLink() } }

    private fun checkLink() {
        val network = currentNetwork ?: run { update(false, "Connect to the sharing phone's Wi-Fi"); return }
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(network)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
            update(false, "Waiting for Wi-Fi connection"); return
        }
        val props = cm.getLinkProperties(network)
        val gateway = props?.routes?.firstOrNull { it.destination.prefixLength == 0 && it.gateway != null }?.gateway
            ?: run { update(false, "Could not identify Wi-Fi gateway"); return }
        val reachable = try {
            Socket().use { it.connect(InetSocketAddress(gateway, ShareService.PORT), 2500); true }
        } catch (_: Exception) { false }
        if (!reachable) { update(false, "Sharing endpoint not responding at $gateway"); return }
        val internet = try {
            val connection = network.openConnection(URL("https://ismyinternetworking.com")) as java.net.HttpURLConnection
            connection.connectTimeout = 4000; connection.readTimeout = 4000; connection.requestMethod = "GET"
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        } catch (_: Exception) { false }
        update(internet, if (internet) "Share endpoint and internet check passed" else "Share endpoint reachable; internet check failed")
    }

    private fun update(healthy: Boolean, message: String) {
        State.setReceive(this, true)
        val notification = Notifications.build(this, "Receiving link", message, healthy)
        (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(12, notification)
    }

    override fun onDestroy() {
        try { (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(callback) } catch (_: Exception) { }
        executor.shutdownNow()
        State.setReceive(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_STOP = "com.pistolwhip.hotspot.STOP_RECEIVE" }
}
