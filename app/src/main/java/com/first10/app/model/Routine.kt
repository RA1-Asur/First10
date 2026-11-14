package com.first10.app.model

data class Routine(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var isDefault: Boolean = true,
    var tasks: MutableList<TaskItem> = mutableListOf()
) {
    fun enabledTasks(): List<TaskItem> = tasks.filter { it.isEnabled }.sortedBy { it.orderIndex }

    fun completedCount(): Int = enabledTasks().count { it.isCompleted }

    fun totalCount(): Int = enabledTasks().size

    fun progressPercent(): Int {
        val total = totalCount()
        if (total == 0) return 0
        return ((completedCount().toFloat() / total) * 100).toInt()
    }

    fun resetDailyCompletion() {
        tasks.forEach { it.isCompleted = false }
    }
}
