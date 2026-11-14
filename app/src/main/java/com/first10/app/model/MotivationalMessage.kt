package com.first10.app.model

data class MotivationalMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String,
    var isEnabled: Boolean = true
)
