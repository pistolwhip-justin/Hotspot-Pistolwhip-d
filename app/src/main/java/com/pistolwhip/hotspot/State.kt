package com.pistolwhip.hotspot

import android.content.Context

object State {
    private const val PREFS = "hotspot_state"
    private const val SHARE = "share_active"
    private const val RECEIVE = "receive_active"
    fun share(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(SHARE, false)
    fun receive(context: Context) = context.getSharedPreferences(PREFS, 0).getBoolean(RECEIVE, false)
    fun setShare(context: Context, value: Boolean) { context.getSharedPreferences(PREFS, 0).edit().putBoolean(SHARE, value).apply() }
    fun setReceive(context: Context, value: Boolean) { context.getSharedPreferences(PREFS, 0).edit().putBoolean(RECEIVE, value).apply() }
}
