package com.example.testing.data

data class SleepData(
    val dataAvailable: Boolean,
    val totalMinutesAsleep: Int,
    val startTime: String?,
    val endTime: String?
)