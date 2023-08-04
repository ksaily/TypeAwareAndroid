package com.example.testing.data

data class KeyboardStats(
    val date: String?,
    val timeWindow: Int?,
    val errors: Double,
    val speed: Double,
    val errorRate: Double,
    val averageWPM: Double,
)