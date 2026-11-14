package com.first10.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.first10.app.R
import com.first10.app.databinding.ActivityPinBinding
import com.first10.app.storage.SecurityManager

class PinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinBinding
    private lateinit var securityManager: SecurityManager

    private val enteredPin = StringBuilder()
    private var tempFirstPin = ""
    private var currentMode = MODE_VERIFY
    private var setupStep = STEP_ENTER_NEW
    private var destinationClass: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val lockoutRunnable = object : Runnable {
        override fun run() {
            if (securityManager.isLockedOut()) {
                val sec = securityManager.remainingLockoutSeconds()
                binding.tvPinError.visibility = View.VISIBLE
                binding.tvPinError.text = getString(R.string.pin_locked, sec)
                binding.gridKeypad.isEnabled = false
                handler.postDelayed(this, 1000L)
            } else {
                binding.gridKeypad.isEnabled = true
                binding.tvPinError.visibility = View.INVISIBLE
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_DESTINATION = "extra_destination"

        const val MODE_VERIFY = 0
        const val MODE_SETUP = 1

        private const val STEP_ENTER_NEW = 0
        private const val STEP_CONFIRM_NEW = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securityManager = SecurityManager(this)
        destinationClass = intent.getStringExtra(EXTRA_DESTINATION)

        val requestedMode = intent.getIntExtra(EXTRA_MODE, MODE_VERIFY)
        currentMode = if (!securityManager.isConfigured()) {
            MODE_SETUP
        } else {
            requestedMode
        }

        setupUI()
        setupKeypad()
    }

    override fun onResume() {
        super.onResume()
        checkLockout()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(lockoutRunnable)
    }

    private fun setupUI() {
        if (currentMode == MODE_SETUP) {
            binding.tvPinTitle.text = getString(R.string.setup_pin)
            binding.tvPinSubtitle.text = "Choose a 4-digit PIN to secure First 10 settings"
            binding.btnForgotPin.visibility = View.GONE
        } else {
            binding.tvPinTitle.text = getString(R.string.enter_pin)
            binding.tvPinSubtitle.text = "Enter your PIN to access protected settings"
            binding.btnForgotPin.visibility = View.VISIBLE
        }
        updateDots()
    }

    private fun setupKeypad() {
        val numberButtons = listOf(
            binding.btnKey0 to "0",
            binding.btnKey1 to "1",
            binding.btnKey2 to "2",
            binding.btnKey3 to "3",
            binding.btnKey4 to "4",
            binding.btnKey5 to "5",
            binding.btnKey6 to "6",
            binding.btnKey7 to "7",
            binding.btnKey8 to "8",
            binding.btnKey9 to "9"
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                if (securityManager.isLockedOut()) return@setOnClickListener
                if (enteredPin.length < 4) {
                    enteredPin.append(digit)
                    updateDots()
                    if (enteredPin.length == 4) {
                        onPinComplete(enteredPin.toString())
                    }
                }
            }
        }

        binding.btnKeyDelete.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                updateDots()
                binding.tvPinError.visibility = View.INVISIBLE
            }
        }

        binding.btnForgotPin.setOnClickListener {
            startActivity(Intent(this, PinRecoveryActivity::class.java))
        }
    }

    private fun updateDots() {
        val len = enteredPin.length
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        for (i in dots.indices) {
            if (i < len) {
                dots[i].backgroundTintList = getColorStateList(R.color.accent_blue)
            } else {
                dots[i].backgroundTintList = getColorStateList(R.color.surface_variant)
            }
        }
    }

    private fun onPinComplete(pin: String) {
        if (currentMode == MODE_SETUP) {
            handleSetupPin(pin)
        } else {
            handleVerifyPin(pin)
        }
    }

    private fun handleVerifyPin(pin: String) {
        if (securityManager.verifyPin(pin)) {
            proceedToDestination()
        } else {
            enteredPin.clear()
            updateDots()
            if (securityManager.isLockedOut()) {
                checkLockout()
            } else {
                binding.tvPinError.visibility = View.VISIBLE
                binding.tvPinError.text = getString(R.string.pin_incorrect, 5)
            }
        }
    }

    private fun handleSetupPin(pin: String) {
        if (setupStep == STEP_ENTER_NEW) {
            tempFirstPin = pin
            enteredPin.clear()
            updateDots()
            setupStep = STEP_CONFIRM_NEW
            binding.tvPinTitle.text = getString(R.string.confirm_pin)
            binding.tvPinSubtitle.text = "Re-enter the 4-digit PIN to confirm"
        } else {
            if (pin == tempFirstPin) {
                promptSecurityRecoveryQuestion(pin)
            } else {
                Toast.makeText(this, getString(R.string.pin_mismatch), Toast.LENGTH_SHORT).show()
                enteredPin.clear()
                tempFirstPin = ""
                setupStep = STEP_ENTER_NEW
                setupUI()
            }
        }
    }

    private fun promptSecurityRecoveryQuestion(pin: String) {
        val input = EditText(this).apply {
            hint = "e.g. My morning focus goal is peace"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Recovery Question")
            .setMessage("If you ever forget your PIN, you can reset it by answering this question:\n\nQuestion: What is your primary morning focus goal?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save PIN") { _, _ ->
                val answer = input.text.toString().trim().ifEmpty { "focus" }
                securityManager.setPin(
                    pin = pin,
                    recoveryQuestion = "What is your primary morning focus goal?",
                    recoveryAnswer = answer
                )
                Toast.makeText(this, "PIN successfully configured", Toast.LENGTH_SHORT).show()
                proceedToDestination()
            }
            .show()
    }

    private fun proceedToDestination() {
        if (!destinationClass.isNullOrEmpty()) {
            try {
                val clazz = Class.forName(destinationClass!!)
                startActivity(Intent(this, clazz))
            } catch (e: Exception) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        } else {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        finish()
    }

    private fun checkLockout() {
        if (securityManager.isLockedOut()) {
            handler.post(lockoutRunnable)
        } else {
            binding.gridKeypad.isEnabled = true
            binding.tvPinError.visibility = View.INVISIBLE
        }
    }
}
