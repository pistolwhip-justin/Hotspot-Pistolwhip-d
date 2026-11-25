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

    fun setShare(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(SHARE, value).apply()
        shareActive.value = value
        if (!value) setShareHealthy(context, false)
    }

    fun setReceive(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(RECEIVE, value).apply()
        receiveActive.value = value
        if (!value) setReceiveHealthy(context, false)
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
