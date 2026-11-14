package com.first10.app.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object MiuiHelper {

    fun isMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
    }

    fun getAutostartIntent(context: Context): Intent {
        val intent = Intent()
        try {
            intent.component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        } catch (ignored: Exception) {}

        // Fallback to app details
        return getAppDetailsIntent(context)
    }

    fun getAppPermEditorIntent(context: Context): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )
        intent.putExtra("extra_pkgname", context.packageName)
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            return intent
        }
        return getAppDetailsIntent(context)
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            return intent
        }
        return getAppDetailsIntent(context)
    }

    fun getAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    private fun getSystemProperty(propName: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $propName")
            process.inputStream.bufferedReader().use { it.readLine() ?: "" }
        } catch (e: Exception) {
            ""
        }
    }
}
