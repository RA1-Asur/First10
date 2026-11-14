package com.first10.app.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    var isRestricted: Boolean = false,
    var isAlwaysAllowed: Boolean = false,
    var isSystemApp: Boolean = false,
    @Transient var icon: Drawable? = null
)
