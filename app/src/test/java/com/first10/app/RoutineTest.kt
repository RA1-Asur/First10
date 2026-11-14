package com.first10.app

import com.first10.app.model.Routine
import com.first10.app.model.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineTest {

    @Test
    fun testRoutineProgressCalculation() {
        val tasks = mutableListOf(
            TaskItem(title = "Task 1", isCompleted = true, isEnabled = true),
            TaskItem(title = "Task 2", isCompleted = false, isEnabled = true),
            TaskItem(title = "Task 3", isCompleted = true, isEnabled = true),
            TaskItem(title = "Task 4", isCompleted = false, isEnabled = true),
            TaskItem(title = "Task 5 (Disabled)", isCompleted = false, isEnabled = false)
        )

        val routine = Routine(name = "Test Routine", tasks = tasks)

        // Only enabled tasks count (4 total)
        assertEquals(4, routine.totalCount())
        assertEquals(2, routine.completedCount())
        assertEquals(50, routine.progressPercent())

        // Complete one more
        tasks[1].isCompleted = true
        assertEquals(3, routine.completedCount())
        assertEquals(75, routine.progressPercent())

        // Reset
        routine.resetDailyCompletion()
        assertEquals(0, routine.completedCount())
        assertEquals(0, routine.progressPercent())
    }
}
