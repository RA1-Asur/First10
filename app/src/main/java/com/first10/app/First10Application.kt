package com.first10.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.first10.app.service.First10MonitoringService
import com.first10.app.storage.PreferencesManager

class First10Application : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val prefs = PreferencesManager.getInstance(this)
            prefs.resetDailyTasksIfNewDay()
            createNotificationChannel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                First10MonitoringService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
