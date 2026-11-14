package com.first10.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.core.StealthManager
import com.first10.app.databinding.ActivityStealthBinding

class StealthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStealthBinding
    private lateinit var stealthManager: StealthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStealthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stealthManager = StealthManager(this)

        setupToolbar()
        setupSwitch()
    }

    private fun setupToolbar() {
        binding.toolbarStealth.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSwitch() {
        binding.switchHideLauncher.isChecked = stealthManager.isLauncherIconHidden()

        binding.switchHideLauncher.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Confirm before hiding launcher icon
                AlertDialog.Builder(this)
                    .setTitle("Hide Launcher Icon?")
                    .setMessage(
                        "This will remove the First 10 icon from your home launcher to reduce temptation.\n\n" +
                        "YOU CAN ALWAYS REOPEN FIRST 10 BY:\n" +
                        "• Dialing *#*#1010#*#* in your phone dialer\n" +
                        "• Going to Android Settings → Apps → First 10 → Tap 'Open'\n\n" +
                        "Do you want to proceed?"
                    )
                    .setPositiveButton("Hide Icon") { _, _ ->
                        stealthManager.setLauncherIconHidden(true)
                        Toast.makeText(this, "Launcher shortcut removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        binding.switchHideLauncher.isChecked = false
                    }
                    .setOnCancelListener {
                        binding.switchHideLauncher.isChecked = false
                    }
                    .show()
            } else {
                stealthManager.setLauncherIconHidden(false)
                Toast.makeText(this, "Launcher shortcut restored", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
