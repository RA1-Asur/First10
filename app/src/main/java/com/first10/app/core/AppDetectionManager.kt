package com.first10.app.core

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.first10.app.model.AppItem
import com.first10.app.storage.PreferencesManager

class AppDetectionManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val packageManager: PackageManager = context.packageManager

    companion object {
        private val ESSENTIAL_DIALER_PACKAGES = setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.google.android.incallui",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui"
        )
    }

    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun getInstalledApps(): List<AppItem> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val restrictedSet = prefs.restrictedPackages
        val allowedSet = prefs.alwaysAllowedPackages
        val myPackage = context.packageName

        val appMap = mutableMapOf<String, AppItem>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackage) continue // Skip First 10 itself

            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val item = AppItem(
                packageName = pkg,
                appName = appName,
                isRestricted = restrictedSet.contains(pkg),
                isAlwaysAllowed = allowedSet.contains(pkg) || isEssentialPackage(pkg),
                isSystemApp = isSystem,
                icon = icon
            )
            appMap[pkg] = item
        }

        return appMap.values.sortedBy { it.appName.lowercase() }
    }

    fun getForegroundPackage(): String? {
        if (!hasUsageAccessPermission()) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L // Query last 10 seconds

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastForegroundPackage = event.packageName
            }
        }

        return lastForegroundPackage
    }

    fun isEssentialPackage(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName == "${context.packageName}.debug") return true
        if (ESSENTIAL_DIALER_PACKAGES.contains(packageName)) return true

        // Check if package handles ACTION_DIAL
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val dialResolvers = packageManager.queryIntentActivities(dialIntent, 0)
        for (info in dialResolvers) {
            if (info.activityInfo.packageName == packageName) {
                return true
            }
        }

        return false
    }

    fun shouldBlockPackage(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        if (packageName == context.packageName || packageName == "${context.packageName}.debug") return false

        // Always Allowed takes strict priority
        if (prefs.alwaysAllowedPackages.contains(packageName)) return false
        if (isEssentialPackage(packageName)) return false

        // Check if restricted
        return prefs.restrictedPackages.contains(packageName)
    }
}
