package com.first10.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.first10.app.core.AppDetectionManager
import com.first10.app.core.BlockingOverlayManager
import com.first10.app.core.RestrictionEngine

class First10AccessibilityService : AccessibilityService() {

    private lateinit var engine: RestrictionEngine
    private lateinit var appDetectionManager: AppDetectionManager
    private lateinit var overlayManager: BlockingOverlayManager

    override fun onCreate() {
        super.onCreate()
        engine = RestrictionEngine.getInstance(this)
        appDetectionManager = AppDetectionManager(this)
        overlayManager = BlockingOverlayManager.getInstance(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (engine.isRestrictionActive()) {
            if (appDetectionManager.shouldBlockPackage(packageName)) {
                overlayManager.showBlockingScreen(packageName)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
