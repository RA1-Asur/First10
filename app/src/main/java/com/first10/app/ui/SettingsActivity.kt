package com.first10.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.R
import com.first10.app.databinding.ActivitySettingsBinding
import com.first10.app.storage.PreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupToolbar()
        setupDurationControls()
        setupNavigationItems()
    }

    override fun onResume() {
        super.onResume()
        updateSummaries()
    }

    private fun setupToolbar() {
        binding.toolbarSettings.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDurationControls() {
        val currentDuration = prefs.restrictionDurationMinutes

        when (currentDuration) {
            5 -> binding.rbDuration5.isChecked = true
            10 -> binding.rbDuration10.isChecked = true
            15 -> binding.rbDuration15.isChecked = true
            else -> {
                binding.rbDurationCustom.isChecked = true
                binding.tilCustomDuration.visibility = View.VISIBLE
                binding.etCustomDuration.setText(currentDuration.toString())
            }
        }

        binding.rgDuration.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbDuration5 -> {
                    binding.tilCustomDuration.visibility = View.GONE
                    prefs.restrictionDurationMinutes = 5
                }
                R.id.rbDuration10 -> {
                    binding.tilCustomDuration.visibility = View.GONE
                    prefs.restrictionDurationMinutes = 10
                }
                R.id.rbDuration15 -> {
                    binding.tilCustomDuration.visibility = View.GONE
                    prefs.restrictionDurationMinutes = 15
                }
                R.id.rbDurationCustom -> {
                    binding.tilCustomDuration.visibility = View.VISIBLE
                    val entered = binding.etCustomDuration.text.toString().toIntOrNull() ?: 10
                    prefs.restrictionDurationMinutes = entered.coerceIn(1, 120)
                }
            }
        }

        binding.etCustomDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull()
                if (value != null && value in 1..120) {
                    prefs.restrictionDurationMinutes = value
                }
            }
        })
    }

    private fun setupNavigationItems() {
        binding.itemRestrictedApps.setOnClickListener {
            val intent = Intent(this, AppManagerActivity::class.java).apply {
                putExtra(AppManagerActivity.EXTRA_TAB, AppManagerActivity.TAB_RESTRICTED)
            }
            startActivity(intent)
        }

        binding.itemAllowedApps.setOnClickListener {
            val intent = Intent(this, AppManagerActivity::class.java).apply {
                putExtra(AppManagerActivity.EXTRA_TAB, AppManagerActivity.TAB_ALLOWED)
            }
            startActivity(intent)
        }

        binding.itemRoutines.setOnClickListener {
            startActivity(Intent(this, RoutineManagerActivity::class.java))
        }

        binding.itemExclusions.setOnClickListener {
            startActivity(Intent(this, ExclusionSchedulerActivity::class.java))
        }

        binding.itemMessages.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        binding.itemChangePin.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java).apply {
                putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_SETUP)
            }
            startActivity(intent)
        }

        binding.itemStealth.setOnClickListener {
            startActivity(Intent(this, StealthActivity::class.java))
        }
    }

    private fun updateSummaries() {
        val restrictedCount = prefs.restrictedPackages.size
        binding.tvSettingsRestrictedSummary.text = "$restrictedCount apps selected to block during focus time"

        val defaultRoutine = prefs.getDefaultRoutine()
        binding.tvSettingsRoutineSummary.text = "${defaultRoutine.enabledTasks().size} morning tasks configured"

        val exclusionCount = prefs.getExclusionPeriods().size
        binding.tvSettingsExclusionSummary.text = "$exclusionCount active exclusion schedules configured"
    }
}
