package com.pistolwhip.hotspot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {
    const val CHANNEL = "hotspot_services"
    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Hotspot services", NotificationManager.IMPORTANCE_LOW))
        }
    }
    fun build(context: Context, title: String, text: String, healthy: Boolean = false): Notification {
        create(context)
        val color = if (healthy) 0xff20c66b.toInt() else 0xffe04444.toInt()
        return if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title).setContentText(text).setColor(color)
            .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()
        else @Suppress("DEPRECATION") android.app.Notification.Builder(context)
            .setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(title).setContentText(text)
            .setOngoing(true).build()
    }
}
