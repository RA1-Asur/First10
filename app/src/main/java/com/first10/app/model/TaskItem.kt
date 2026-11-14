package com.first10.app.model

data class TaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    var description: String = "",
    var isCompleted: Boolean = false,
    var isEnabled: Boolean = true,
    var orderIndex: Int = 0
)
