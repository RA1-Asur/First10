package com.first10.app.core

import android.content.Context
import com.first10.app.model.ExclusionPeriod
import com.first10.app.storage.PreferencesManager
import java.util.Calendar
import java.util.Locale

class RestrictionEngine(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    companion object {
        @Volatile
        private var instance: RestrictionEngine? = null

        fun getInstance(context: Context): RestrictionEngine {
            return instance ?: synchronized(this) {
                instance ?: RestrictionEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Determines whether a successful unlock event qualifies to trigger the restriction.
     */
    fun shouldActivateOnUnlock(): Boolean {
        // 1. Is First 10 master enabled?
        if (!prefs.isAppEnabled) return false

        // 2. Is a restriction already active right now?
        if (isRestrictionActive()) return false

        // 3. Has today's quota already been consumed?
        val today = prefs.getTodayDateString()
        if (prefs.lastActivationDate == today) {
            return false
        }

        // 4. Is the current unlock during an active exclusion period?
        val calendar = Calendar.getInstance()
        if (isWithinExclusionPeriod(calendar, prefs.getExclusionPeriods())) {
            // Unlock occurred during exclusion period; do not start & do not consume today's quota
            return false
        }

        return true
    }

    fun isWithinExclusionPeriod(calendar: Calendar, exclusions: List<ExclusionPeriod>): Boolean {
        for (exclusion in exclusions) {
            if (exclusion.matchesTime(calendar)) {
                return true
            }
        }
        return false
    }

    /**
     * Starts the daily restriction period and persists timestamps.
     */
    fun startRestriction(durationMinutes: Int = prefs.restrictionDurationMinutes): Long {
        val now = System.currentTimeMillis()
        val durationMillis = durationMinutes * 60 * 1000L
        val expiration = now + durationMillis

        prefs.startTimestamp = now
        prefs.expirationTimestamp = expiration
        prefs.isRestrictionActive = true
        prefs.lastActivationDate = prefs.getTodayDateString()

        // Reset today's task checklist for the new morning session
        val routines = prefs.getRoutines()
        routines.forEach { it.resetDailyCompletion() }
        prefs.saveRoutines(routines)

        prefs.logTamperEvent(
            "RESTRICTION_STARTED",
            "Morning focus period started for $durationMinutes minutes. Expires at $expiration"
        )

        return expiration
    }

    fun stopRestriction(reason: String = "EXPIRED") {
        prefs.isRestrictionActive = false
        prefs.logTamperEvent("RESTRICTION_STOPPED", "Morning focus period stopped: $reason")
    }

    fun isRestrictionActive(): Boolean {
        val active = prefs.isRestrictionActive
        val expires = prefs.expirationTimestamp
        val now = System.currentTimeMillis()
        if (active && now >= expires) {
            // Automatically clean up expired state
            prefs.isRestrictionActive = false
            return false
        }
        return active && now < expires
    }

    fun getRemainingMillis(): Long {
        if (!isRestrictionActive()) return 0L
        val remaining = prefs.expirationTimestamp - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun formatRemainingTime(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
