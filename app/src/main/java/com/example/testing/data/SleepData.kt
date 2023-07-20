package com.example.testing.data

import org.json.JSONObject

data class SleepData(
    val dataAvailable: Boolean,
    val totalMinutesAsleep: Int,
    val startTime: String?,
    val endTime: String?,
    val fullSleepData: Map<String, Any>
)