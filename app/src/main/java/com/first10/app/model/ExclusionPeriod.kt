package com.first10.app.model

import java.util.Calendar
import java.util.Locale

data class ExclusionPeriod(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var startHour: Int,
    var startMinute: Int,
    var endHour: Int,
    var endMinute: Int,
    var daysOfWeek: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    ),
    var isEnabled: Boolean = true
) {
    fun isOvernight(): Boolean {
        return startHour > endHour || (startHour == endHour && startMinute > endMinute)
    }

    fun formattedTimeRange(): String {
        val startStr = formatTime(startHour, startMinute)
        val endStr = formatTime(endHour, endMinute)
        return if (isOvernight()) {
            "$startStr – $endStr (Overnight)"
        } else {
            "$startStr – $endStr"
        }
    }

    fun formattedDays(): String {
        if (daysOfWeek.size == 7) return "Every day"
        val dayNames = listOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        return dayNames.filter { daysOfWeek.contains(it.first) }
            .joinToString(", ") { it.second }
    }

    fun matchesTime(cal: Calendar): Boolean {
        if (!isEnabled) return false

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (isOvernight()) {
            // E.g. 22:00 to 06:00
            // If current time is >= 22:00, check today's day of week
            if (currentMinutes >= startMinutes) {
                daysOfWeek.contains(dayOfWeek)
            } else if (currentMinutes <= endMinutes) {
                // If current time is <= 06:00, check yesterday's day of week
                val prevDay = if (dayOfWeek == Calendar.SUNDAY) Calendar.SATURDAY else dayOfWeek - 1
                daysOfWeek.contains(prevDay)
            } else {
                false
            }
        } else {
            // E.g. 14:00 to 16:00
            if (!daysOfWeek.contains(dayOfWeek)) return false
            currentMinutes in startMinutes..endMinutes
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }
}
