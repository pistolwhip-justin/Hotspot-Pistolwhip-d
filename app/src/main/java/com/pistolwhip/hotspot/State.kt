package com.pistolwhip.hotspot

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

object State {
    private const val PREFS = "hotspot_state"
    private const val SHARE = "share_active"
    private const val RECEIVE = "receive_active"
    private const val TUNNEL = "tunnel_active"
    private const val SHARE_HEALTHY = "share_healthy"
    private const val RECEIVE_HEALTHY = "receive_healthy"

    val shareActive = MutableStateFlow(false)
    val receiveActive = MutableStateFlow(false)
    val shareHealthy = MutableStateFlow(false)
    val receiveHealthy = MutableStateFlow(false)
    val shareSsid = MutableStateFlow("")
    val sharePassphrase = MutableStateFlow("")
    val shareAddress = MutableStateFlow("")
    val sharePeerCount = MutableStateFlow(0)
    val receiveMessage = MutableStateFlow("Not connected")
    val receiveSsid = MutableStateFlow("")

    fun share(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(SHARE, false)
    fun receive(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(RECEIVE, false)
    fun tunnel(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(TUNNEL, false)

    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        shareActive.value = prefs.getBoolean(SHARE, false)
        receiveActive.value = prefs.getBoolean(RECEIVE, false)
        shareHealthy.value = prefs.getBoolean(SHARE_HEALTHY, false)
        receiveHealthy.value = prefs.getBoolean(RECEIVE_HEALTHY, false)
    }

    fun setShareCredentials(ssid: String, passphrase: String) {
        shareSsid.value = ssid
        sharePassphrase.value = passphrase
    }

    fun clearShareCredentials() {
        shareSsid.value = ""
        sharePassphrase.value = ""
    }

    fun setShareAddress(address: String) { shareAddress.value = address }
    fun setSharePeerCount(count: Int) { sharePeerCount.value = count.coerceAtLeast(0) }
    fun setReceiveMessage(message: String) { receiveMessage.value = message }
    fun setReceiveSsid(ssid: String) { receiveSsid.value = ssid }

    fun setShare(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(SHARE, value).apply()
        shareActive.value = value
        if (!value) { setShareHealthy(context, false); clearShareCredentials(); setShareAddress(""); setSharePeerCount(0) }
    }

    fun setReceive(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(RECEIVE, value).apply()
        receiveActive.value = value
        if (!value) { setReceiveHealthy(context, false); setReceiveMessage("Not connected"); setReceiveSsid("") }
    }

    fun setTunnel(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(TUNNEL, value).apply()
    }

    fun setShareHealthy(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(SHARE_HEALTHY, value).apply()
        shareHealthy.value = value
    }

    fun setReceiveHealthy(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(RECEIVE_HEALTHY, value).apply()
        receiveHealthy.value = value
    }
}
