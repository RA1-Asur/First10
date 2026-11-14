package com.first10.app.core

import android.content.Context
import android.content.Intent
import com.first10.app.ui.BlockingActivity

class BlockingOverlayManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: BlockingOverlayManager? = null

        fun getInstance(context: Context): BlockingOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: BlockingOverlayManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var isOverlayShowing = false

    fun showBlockingScreen(blockedPackage: String? = null) {
        if (isOverlayShowing) return

        isOverlayShowing = true
        val intent = Intent(context, BlockingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra("BLOCKED_PACKAGE", blockedPackage)
        }
        context.startActivity(intent)
    }

    fun dismissBlockingScreen() {
        isOverlayShowing = false
        // Broadcast to BlockingActivity to finish itself if alive
        val intent = Intent("com.first10.app.ACTION_DISMISS_BLOCKING")
        context.sendBroadcast(intent)
    }

    fun onBlockingActivityDestroyed() {
        isOverlayShowing = false
    }

    fun isShowing(): Boolean = isOverlayShowing
}
