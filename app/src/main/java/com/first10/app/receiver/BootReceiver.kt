package com.first10.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.first10.app.core.RestrictionEngine
import com.first10.app.service.First10MonitoringService
import com.first10.app.storage.PreferencesManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val prefs = PreferencesManager.getInstance(context)
        prefs.logTamperEvent("DEVICE_BOOT", "Device reboot detected")

        val engine = RestrictionEngine.getInstance(context)
        if (engine.isRestrictionActive()) {
            val serviceIntent = Intent(context, First10MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
