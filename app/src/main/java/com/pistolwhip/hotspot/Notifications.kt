package com.pistolwhip.hotspot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    const val CHANNEL = "hotspot_services"
    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Hotspot services", NotificationManager.IMPORTANCE_LOW))
    }
    fun build(context: Context, title: String, text: String, healthy: Boolean = false): Notification {
        create(context)
        val color = if (healthy) 0xff20c66b.toInt() else 0xffe04444.toInt()
        return Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title).setContentText(text).setColor(color)
            .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()
    }
}
