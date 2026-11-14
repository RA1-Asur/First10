package com.first10.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.first10.app.core.RestrictionEngine
import com.first10.app.service.First10MonitoringService

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_USER_PRESENT) return

        val engine = RestrictionEngine.getInstance(context)

        if (engine.shouldActivateOnUnlock()) {
            engine.startRestriction()

            val serviceIntent = Intent(context, First10MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
