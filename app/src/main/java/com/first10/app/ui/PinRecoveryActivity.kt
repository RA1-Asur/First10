package com.first10.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.databinding.ActivityPinRecoveryBinding
import com.first10.app.storage.PreferencesManager
import com.first10.app.storage.SecurityManager

class PinRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinRecoveryBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        securityManager = SecurityManager(this)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        binding.toolbarPinRecovery.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        binding.tvRecoveryQuestion.text = "Security Question:\n${prefs.recoveryQuestion}"

        binding.btnResetPin.setOnClickListener {
            val answer = binding.etRecoveryAnswer.text.toString().trim()
            val newPin = binding.etNewPin.text.toString().trim()

            if (answer.isEmpty()) {
                Toast.makeText(this, "Please enter your recovery answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPin.length != 4) {
                Toast.makeText(this, "New PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (securityManager.verifyRecoveryAnswer(answer)) {
                securityManager.resetPinWithRecovery(newPin)
                Toast.makeText(this, "PIN successfully reset! You can now log in.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Incorrect recovery answer. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
