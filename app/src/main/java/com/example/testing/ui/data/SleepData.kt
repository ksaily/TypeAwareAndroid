package com.example.testing.ui.data

data class SleepData(
    val dataAvailable: Boolean,
    val totalMinutesAsleep: Int,
    val startTime: String?,
    val endTime: String?
)