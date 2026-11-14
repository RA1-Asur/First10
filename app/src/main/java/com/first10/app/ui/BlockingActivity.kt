package com.first10.app.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.first10.app.core.BlockingOverlayManager
import com.first10.app.core.RestrictionEngine
import com.first10.app.databinding.ActivityBlockingBinding
import com.first10.app.storage.PreferencesManager
import com.first10.app.ui.adapter.BlockingTaskAdapter

class BlockingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockingBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var engine: RestrictionEngine
    private lateinit var taskAdapter: BlockingTaskAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var holdStartTime = 0L
    private var isHoldingEmergency = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!engine.isRestrictionActive()) {
                finish()
                return
            }
            updateTimerDisplay()
            handler.postDelayed(this, 1000L)
        }
    }

    private val holdProgressRunnable = object : Runnable {
        override fun run() {
            if (!isHoldingEmergency) return
            val elapsed = System.currentTimeMillis() - holdStartTime
            val percent = ((elapsed.toFloat() / 10_000L) * 100).toInt().coerceIn(0, 100)
            binding.progressEmergencyHold.progress = percent

            val remainingSec = ((10_000L - elapsed) / 1000L) + 1
            binding.btnEmergencyBypass.text = "Holding: ${remainingSec}s…"

            if (elapsed >= 10_000L) {
                isHoldingEmergency = false
                binding.progressEmergencyHold.visibility = View.GONE
                binding.btnEmergencyBypass.text = "Hold for Emergency Bypass (10s)"
                showEmergencyConfirmDialog()
            } else {
                handler.postDelayed(this, 100L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        engine = RestrictionEngine.getInstance(this)

        setupUI()
        setupEmergencyButton()

        val filter = IntentFilter("com.first10.app.ACTION_DISMISS_BLOCKING")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(timerRunnable)
        updateTasksProgress()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
        cancelEmergencyHold()
    }

    override fun onDestroy() {
        super.onDestroy()
        BlockingOverlayManager.getInstance(this).onBlockingActivityDestroyed()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (ignored: Exception) {}
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Pressing back takes user to home launcher rather than allowing them back into the restricted app
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun setupUI() {
        // Motivational message
        binding.tvMotivationalMessage.text = prefs.getActiveMotivationalMessage()

        // Setup Task RecyclerView
        val routine = prefs.getDefaultRoutine()
        val enabledTasks = routine.enabledTasks()

        taskAdapter = BlockingTaskAdapter(enabledTasks) { task, isChecked ->
            prefs.updateTaskCompletion(task.id, isChecked)
            updateTasksProgress()
        }

        binding.rvBlockingTasks.apply {
            layoutManager = LinearLayoutManager(this@BlockingActivity)
            adapter = taskAdapter
        }

        updateTasksProgress()
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val remaining = engine.getRemainingMillis()
        if (remaining <= 0) {
            binding.tvBlockingTimer.text = "00:00"
            finish()
        } else {
            binding.tvBlockingTimer.text = engine.formatRemainingTime(remaining)
        }
    }

    private fun updateTasksProgress() {
        val routine = prefs.getDefaultRoutine()
        val completed = routine.completedCount()
        val total = routine.totalCount()
        val percent = routine.progressPercent()

        binding.tvBlockingTaskProgress.text = "$completed of $total ($percent%)"
        binding.progressBlockingTasks.progress = percent
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEmergencyButton() {
        binding.btnEmergencyBypass.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startEmergencyHold()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelEmergencyHold()
                    true
                }
                else -> false
            }
        }
    }

    private fun startEmergencyHold() {
        isHoldingEmergency = true
        holdStartTime = System.currentTimeMillis()
        binding.progressEmergencyHold.visibility = View.VISIBLE
        binding.progressEmergencyHold.progress = 0
        handler.post(holdProgressRunnable)
    }

    private fun cancelEmergencyHold() {
        if (isHoldingEmergency) {
            isHoldingEmergency = false
            handler.removeCallbacks(holdProgressRunnable)
            binding.progressEmergencyHold.visibility = View.GONE
            binding.btnEmergencyBypass.text = "Hold for Emergency Bypass (10s)"
        }
    }

    private fun showEmergencyConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Emergency Bypass")
            .setMessage("Are you sure you want to bypass today's morning restriction? This event will be logged in your security history.")
            .setPositiveButton("Bypass Now") { _, _ ->
                engine.stopRestriction("EMERGENCY_BYPASS")
                prefs.logTamperEvent("EMERGENCY_OVERRIDE", "Emergency bypass confirmed by user after 10s hold")
                Toast.makeText(this, "Restriction bypassed for today", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("Keep Focus", null)
            .setCancelable(false)
            .show()
    }
}
