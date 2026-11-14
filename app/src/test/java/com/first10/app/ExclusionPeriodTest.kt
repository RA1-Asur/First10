package com.first10.app

import com.first10.app.model.ExclusionPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExclusionPeriodTest {

    @Test
    fun testSameDayExclusion() {
        val period = ExclusionPeriod(
            name = "Afternoon Study",
            startHour = 14,
            startMinute = 0,
            endHour = 16,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY),
            isEnabled = true
        )

        assertFalse(period.isOvernight())

        // Monday at 15:00 should match
        val calMatch = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(period.matchesTime(calMatch))

        // Monday at 17:00 should NOT match
        val calOutsideTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(period.matchesTime(calOutsideTime))

        // Thursday at 15:00 should NOT match (day not in set)
        val calOutsideDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(period.matchesTime(calOutsideDay))
    }

    @Test
    fun testOvernightExclusionCrossingMidnight() {
        val period = ExclusionPeriod(
            name = "Overnight Sleep",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            daysOfWeek = setOf(Calendar.FRIDAY), // Friday night to Saturday morning
            isEnabled = true
        )

        assertTrue(period.isOvernight())

        // Friday 23:30 should match
        val calFridayNight = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(period.matchesTime(calFridayNight))

        // Saturday 04:00 should match (spills over from Friday night)
        val calSaturdayMorning = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(period.matchesTime(calSaturdayMorning))

        // Saturday 07:00 should NOT match
        val calSaturdayLate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(period.matchesTime(calSaturdayLate))
    }

    @Test
    fun testDisabledExclusionDoesNotMatch() {
        val period = ExclusionPeriod(
            name = "Disabled Test",
            startHour = 0,
            startMinute = 0,
            endHour = 23,
            endMinute = 59,
            isEnabled = false
        )

        val cal = Calendar.getInstance()
        assertFalse(period.matchesTime(cal))
    }
}
