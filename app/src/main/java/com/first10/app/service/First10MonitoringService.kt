package com.first10.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.first10.app.R
import com.first10.app.core.AppDetectionManager
import com.first10.app.core.BlockingOverlayManager
import com.first10.app.core.RestrictionEngine
import com.first10.app.ui.MainActivity

class First10MonitoringService : Service() {

    private lateinit var engine: RestrictionEngine
    private lateinit var appDetectionManager: AppDetectionManager
    private lateinit var overlayManager: BlockingOverlayManager
    private lateinit var telephonyManager: TelephonyManager

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    companion object {
        const val CHANNEL_ID = "first10_channel"
        const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 600L
    }

    override fun onCreate() {
        super.onCreate()
        engine = RestrictionEngine.getInstance(this)
        appDetectionManager = AppDetectionManager(this)
        overlayManager = BlockingOverlayManager.getInstance(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(engine.formatRemainingTime(engine.getRemainingMillis()))
        startForeground(NOTIFICATION_ID, notification)

        if (!isMonitoring) {
            isMonitoring = true
            startMonitoringLoop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoringLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!engine.isRestrictionActive()) {
                    // Timer expired!
                    overlayManager.dismissBlockingScreen()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    isMonitoring = false
                    return
                }

                // Check active calls - calls ALWAYS take precedence
                val callState = try {
                    @Suppress("DEPRECATION")
                    telephonyManager.callState
                } catch (e: Exception) {
                    TelephonyManager.CALL_STATE_IDLE
                }

                if (callState != TelephonyManager.CALL_STATE_IDLE) {
                    // Ongoing or incoming call - do not block
                    if (overlayManager.isShowing()) {
                        overlayManager.dismissBlockingScreen()
                    }
                } else {
                    // Normal monitoring
                    val foregroundPackage = appDetectionManager.getForegroundPackage()
                    if (appDetectionManager.shouldBlockPackage(foregroundPackage)) {
                        overlayManager.showBlockingScreen(foregroundPackage)
                    }
                }

                // Update notification countdown
                val remaining = engine.getRemainingMillis()
                updateNotification(engine.formatRemainingTime(remaining))

                // Schedule next check
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
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

    private fun buildNotification(remainingText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_active))
            .setContentText(getString(R.string.notification_text_active, remainingText))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(remainingText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(remainingText))
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
    }
}
