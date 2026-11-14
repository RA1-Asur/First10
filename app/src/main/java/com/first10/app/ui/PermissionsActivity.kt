package com.first10.app.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.R
import com.first10.app.core.AppDetectionManager
import com.first10.app.core.MiuiHelper
import com.first10.app.databinding.ActivityPermissionsBinding
import com.first10.app.receiver.First10DeviceAdminReceiver

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var appDetectionManager: AppDetectionManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDetectionManager = AppDetectionManager(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, First10DeviceAdminReceiver::class.java)

        setupToolbar()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
    }

    private fun setupToolbar() {
        binding.toolbarPermissions.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        // 1. Usage Access
        binding.btnGrantUsageAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        // 2. Overlay
        binding.btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // 3. Device Admin
        binding.btnGrantAdmin.setOnClickListener {
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "First 10 Device Administrator prevents impulsive deactivation before your morning routine finishes."
                    )
                }
                startActivity(intent)
            } else {
                // If already active, open settings to manage
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        }

        // 4. Battery Optimization
        binding.btnGrantBattery.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        // 5. MIUI Guide
        binding.btnOpenMiuiGuide.setOnClickListener {
            startActivity(Intent(this, MiuiGuideActivity::class.java))
        }

        // Show MIUI card if on Xiaomi / Redmi
        if (MiuiHelper.isMiui()) {
            binding.cardMiuiGuide.visibility = android.view.View.VISIBLE
        }
    }

    private fun updatePermissionStatuses() {
        // 1. Usage Access
        val hasUsage = appDetectionManager.hasUsageAccessPermission()
        updateBadge(binding.tvStatusUsageAccess, hasUsage)

        // 2. Overlay
        val hasOverlay = appDetectionManager.hasOverlayPermission()
        updateBadge(binding.tvStatusOverlay, hasOverlay)

        // 3. Device Admin
        val hasAdmin = devicePolicyManager.isAdminActive(adminComponent)
        updateBadge(binding.tvStatusAdmin, hasAdmin)
        binding.btnGrantAdmin.text = if (hasAdmin) "Active (Tap to manage)" else getString(R.string.btn_grant)

        // 4. Battery
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val hasBatteryExemption = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
        } else true
        updateBadge(binding.tvStatusBattery, hasBatteryExemption)
    }

    private fun updateBadge(textView: android.widget.TextView, isGranted: Boolean) {
        if (isGranted) {
            textView.text = getString(R.string.status_granted)
            textView.setTextColor(getColor(R.color.accent_green))
        } else {
            textView.text = getString(R.string.status_missing)
            textView.setTextColor(getColor(R.color.error))
        }
    }
}
