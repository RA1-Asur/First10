package com.first10.app.storage

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

class SecurityManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
    }

    fun isConfigured(): Boolean {
        return prefs.hasPinConfigured
    }

    fun isLockedOut(): Boolean {
        return System.currentTimeMillis() < prefs.lockoutUntil
    }

    fun remainingLockoutSeconds(): Int {
        val diff = prefs.lockoutUntil - System.currentTimeMillis()
        return if (diff > 0) ((diff / 1000) + 1).toInt() else 0
    }

    fun setPin(pin: String, recoveryQuestion: String, recoveryAnswer: String) {
        val salt = generateSalt()
        val hash = hashString(pin, salt)

        prefs.pinSalt = salt
        prefs.pinHash = hash
        prefs.recoveryQuestion = recoveryQuestion
        prefs.recoveryAnswerHash = hashString(recoveryAnswer.trim().lowercase(Locale.ROOT), salt)

        // Reset failed counters
        prefs.failedPinAttempts = 0
        prefs.lockoutUntil = 0L

        prefs.logTamperEvent("PIN_CHANGED", "Security PIN updated by owner")
    }

    fun verifyPin(pin: String): Boolean {
        if (isLockedOut()) return false

        val storedHash = prefs.pinHash ?: return false
        val storedSalt = prefs.pinSalt ?: return false

        val inputHash = hashString(pin, storedSalt)
        return if (inputHash == storedHash) {
            prefs.failedPinAttempts = 0
            prefs.lockoutUntil = 0L
            true
        } else {
            val failed = prefs.failedPinAttempts + 1
            prefs.failedPinAttempts = failed
            if (failed >= MAX_FAILED_ATTEMPTS) {
                prefs.lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                prefs.logTamperEvent("PIN_LOCKOUT", "5 consecutive incorrect PIN attempts. Locked for 30s")
            } else {
                prefs.logTamperEvent("PIN_FAILED", "Incorrect PIN attempt ($failed/$MAX_FAILED_ATTEMPTS)")
            }
            false
        }
    }

    fun verifyRecoveryAnswer(answer: String): Boolean {
        val storedHash = prefs.recoveryAnswerHash ?: return false
        val storedSalt = prefs.pinSalt ?: return false

        val inputHash = hashString(answer.trim().lowercase(Locale.ROOT), storedSalt)
        return inputHash == storedHash
    }

    fun resetPinWithRecovery(newPin: String): Boolean {
        val salt = generateSalt()
        val hash = hashString(newPin, salt)

        prefs.pinSalt = salt
        prefs.pinHash = hash
        prefs.failedPinAttempts = 0
        prefs.lockoutUntil = 0L

        prefs.logTamperEvent("PIN_RESET_RECOVERY", "PIN reset via security recovery answer")
        return true
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return bytesToHex(saltBytes)
    }

    private fun hashString(input: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
