package com.first10.app.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.first10.app.storage.PreferencesManager

class StealthManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    fun isLauncherIconHidden(): Boolean {
        val aliasComponent = ComponentName(context, "com.first10.app.MainActivityAlias")
        val state = context.packageManager.getComponentEnabledSetting(aliasComponent)
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setLauncherIconHidden(hide: Boolean) {
        val aliasComponent = ComponentName(context, "com.first10.app.MainActivityAlias")
        val newState = if (hide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        context.packageManager.setComponentEnabledSetting(
            aliasComponent,
            newState,
            PackageManager.DONT_KILL_APP
        )

        prefs.isLauncherHidden = hide
        prefs.logTamperEvent(
            "STEALTH_MODE_TOGGLED",
            if (hide) "Launcher shortcut hidden" else "Launcher shortcut restored"
        )
    }
}
