package com.example.testing.utils

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Clicks(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "device_id") val deviceId: String?,
    @ColumnInfo(name = "time") val time: Int?,
    @ColumnInfo(name = "new_text") val newText: String?,
    @ColumnInfo(name = "before_text") val beforeText: String?
)