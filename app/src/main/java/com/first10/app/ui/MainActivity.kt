package com.first10.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.R
import com.first10.app.core.AppDetectionManager
import com.first10.app.core.RestrictionEngine
import com.first10.app.databinding.ActivityMainBinding
import com.first10.app.storage.PreferencesManager
import com.first10.app.storage.SecurityManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var engine: RestrictionEngine
    private lateinit var securityManager: SecurityManager
    private lateinit var appDetectionManager: AppDetectionManager

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateDashboardState()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        engine = RestrictionEngine.getInstance(this)
        securityManager = SecurityManager(this)
        appDetectionManager = AppDetectionManager(this)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateDashboardState()
        handler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    private fun setupListeners() {
        binding.btnOpenSettings.setOnClickListener {
            // Require PIN before opening settings
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra(PinActivity.EXTRA_DESTINATION, SettingsActivity::class.java.name)
            }
            startActivity(intent)
        }

        binding.btnTestOverlay.setOnClickListener {
            val intent = Intent(this, BlockingActivity::class.java)
            startActivity(intent)
        }

        binding.btnPermissionsOverview.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }

        binding.btnFixPermissions.setOnClickListener {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }

        binding.cardRestrictedStat.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra(PinActivity.EXTRA_DESTINATION, AppManagerActivity::class.java.name)
            }
            startActivity(intent)
        }

        binding.cardAllowedStat.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra(PinActivity.EXTRA_DESTINATION, AppManagerActivity::class.java.name)
            }
            startActivity(intent)
        }

        binding.cardRoutineSummary.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra(PinActivity.EXTRA_DESTINATION, RoutineManagerActivity::class.java.name)
            }
            startActivity(intent)
        }
    }

    private fun updateDashboardState() {
        val isActive = engine.isRestrictionActive()

        if (isActive) {
            val remainingMillis = engine.getRemainingMillis()
            val remainingText = engine.formatRemainingTime(remainingMillis)

            binding.tvStatusTitle.text = getString(R.string.status_protected)
            binding.tvStatusTitle.setTextColor(getColor(R.color.accent_green))
            binding.indicatorStatus.backgroundTintList = getColorStateList(R.color.accent_green)

            binding.tvCountdown.visibility = View.VISIBLE
            binding.tvCountdown.text = remainingText
            binding.tvStatusDesc.text = "Morning restriction is in progress. Selected distracting applications are blocked."
        } else {
            binding.tvStatusTitle.text = getString(R.string.status_idle)
            binding.tvStatusTitle.setTextColor(getColor(R.color.on_surface))
            binding.indicatorStatus.backgroundTintList = getColorStateList(R.color.accent_blue)

            binding.tvCountdown.visibility = View.GONE
            val durationMin = prefs.restrictionDurationMinutes
            val lastDate = prefs.lastActivationDate
            val today = prefs.getTodayDateString()

            if (lastDate == today) {
                binding.tvStatusDesc.text = "Today's $durationMin-minute morning focus period is completed. You're set for the day!"
            } else {
                binding.tvStatusDesc.text = "Your next qualifying unlock today will start the $durationMin-minute focus protection period."
            }
        }

        // Stats
        binding.tvRestrictedCount.text = prefs.restrictedPackages.size.toString()
        binding.tvAllowedCount.text = "${prefs.alwaysAllowedPackages.size} Allowed"

        // Routine completion
        val routine = prefs.getDefaultRoutine()
        val percent = routine.progressPercent()
        binding.tvRoutinePercent.text = "$percent%"
        binding.progressRoutine.progress = percent
        binding.tvRoutineTaskCount.text = "${routine.completedCount()} of ${routine.totalCount()} morning tasks completed"

        // Permission warning
        val hasUsage = appDetectionManager.hasUsageAccessPermission()
        val hasOverlay = appDetectionManager.hasOverlayPermission()
        if (!hasUsage || !hasOverlay) {
            binding.cardPermissionWarning.visibility = View.VISIBLE
        } else {
            binding.cardPermissionWarning.visibility = View.GONE
        }
    }
}
