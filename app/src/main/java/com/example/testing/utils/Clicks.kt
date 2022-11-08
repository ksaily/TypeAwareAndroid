package com.example.testing.utils

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

data class Clicks(
    val timestamp: Long? = 0,
    val packageName: String? = null,
    val newText: String? = null,
    val beforeText: String? = null,
    val isPassword: Boolean? = false
)


