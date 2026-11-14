package com.first10.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.first10.app.storage.PreferencesManager

class First10DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val prefs = PreferencesManager.getInstance(context)
        prefs.logTamperEvent("DEVICE_ADMIN_ENABLED", "Device Administrator activated")
        Toast.makeText(context, "First 10 Administrator Protection Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val prefs = PreferencesManager.getInstance(context)
        prefs.logTamperEvent("DEVICE_ADMIN_DISABLE_REQUESTED", "Device Administrator deactivation requested")
        return "Disabling Device Administrator removes protection against impulsive uninstallation of First 10."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val prefs = PreferencesManager.getInstance(context)
        prefs.logTamperEvent("DEVICE_ADMIN_DISABLED", "Device Administrator deactivated")
        Toast.makeText(context, "First 10 Administrator Protection Deactivated", Toast.LENGTH_SHORT).show()
    }
}
