package com.example.testing.utils

data class KeyboardEvents(
    val id: String = "",
    val wordCount: Int = 0,
    val typingSpeed: ArrayList<Double>,
    val errorAmount: Int = 0,
    val errorRate: Double = 0.0,
    val timeStampBeginning: Long = 0,
    val timeStampEnd: Long = 0,
    val sessionPackage: String = "",
    val beforeText: CharSequence = "",
    val dailyTimeWindow: Int = 0,
    val day: Int = 0
)


