package com.example.testing.utils

import android.provider.ContactsContract.Data
import java.util.*
import kotlin.collections.ArrayList

data class KeyboardEvents(
    val id: String?,
    val wordCount: Int,
    val typingSpeed: ArrayList<Double>,
    val errorAmount: Int,
    val errorRate: Double,
    val timeStampBeginning: Long,
    val timeStampEnd: Long,
    val sessionPackage: String,
    val beforeText: CharSequence,
    val dailyTimeWindow: Int,
    val date: String
)



