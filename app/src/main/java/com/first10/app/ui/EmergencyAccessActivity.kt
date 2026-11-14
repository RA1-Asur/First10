package com.first10.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.core.RestrictionEngine
import com.first10.app.databinding.ActivityEmergencyAccessBinding
import com.first10.app.storage.PreferencesManager

class EmergencyAccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyAccessBinding
    private lateinit var engine: RestrictionEngine
    private lateinit var prefs: PreferencesManager

    private val handler = Handler(Looper.getMainLooper())
    private var holdStartTime = 0L
    private var isHolding = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!isHolding) return
            val elapsed = System.currentTimeMillis() - holdStartTime
            val percent = ((elapsed.toFloat() / 10_000L) * 100).toInt().coerceIn(0, 100)
            binding.progressCircularHold.progress = percent

            val seconds = (elapsed / 1000L).coerceAtMost(10L)
            binding.tvHoldSeconds.text = "$seconds / 10s"

            if (elapsed >= 10_000L) {
                isHolding = false
                showConfirmationDialog()
            } else {
                handler.postDelayed(this, 100L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = RestrictionEngine.getInstance(this)
        prefs = PreferencesManager.getInstance(this)

        setupListeners()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.btnHoldEmergency.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isHolding = true
                    holdStartTime = System.currentTimeMillis()
                    handler.post(progressRunnable)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelHold()
                    true
                }
                else -> false
            }
        }

        binding.btnCancelEmergency.setOnClickListener {
            finish()
        }
    }

    private fun cancelHold() {
        if (isHolding) {
            isHolding = false
            handler.removeCallbacks(progressRunnable)
            binding.progressCircularHold.progress = 0
            binding.tvHoldSeconds.text = "0 / 10s"
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Emergency Access")
            .setMessage("Do you confirm you have an urgent situation that requires immediate app access? This will cancel today's morning restriction and be recorded in your security audit log.")
            .setPositiveButton("Bypass Restriction") { _, _ ->
                engine.stopRestriction("EMERGENCY_ACCESS_SCREEN")
                prefs.logTamperEvent("EMERGENCY_OVERRIDE", "Emergency bypass confirmed via Emergency Access Screen")
                Toast.makeText(this, "Restriction bypassed for today", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    override fun onPause() {
        super.onPause()
        cancelHold()
    }
}
