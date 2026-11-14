package com.first10.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.core.MiuiHelper
import com.first10.app.databinding.ActivityMiuiGuideBinding

class MiuiGuideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMiuiGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMiuiGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbarMiui.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        // Step 1: Autostart
        binding.btnOpenAutostart.setOnClickListener {
            try {
                val intent = MiuiHelper.getAutostartIntent(this)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Autostart automatically. Open Security app → Permissions → Autostart.", Toast.LENGTH_LONG).show()
            }
        }

        // Step 2: Battery Saver
        binding.btnOpenBatterySaver.setOnClickListener {
            try {
                val intent = MiuiHelper.getBatteryOptimizationIntent(this)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Battery Optimization. Go to Settings → Battery → Battery Saver.", Toast.LENGTH_LONG).show()
            }
        }

        // Step 3: App Permissions (Pop-up windows)
        binding.btnOpenAppPermissions.setOnClickListener {
            try {
                val intent = MiuiHelper.getAppPermEditorIntent(this)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Permissions Editor. Go to Settings → Apps → First 10 → Permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
