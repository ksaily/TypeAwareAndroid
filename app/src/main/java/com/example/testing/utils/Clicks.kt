package com.example.testing.utils

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Clicks(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "device_id") val deviceId: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Int?,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "new_text") val newText: String?,
    @ColumnInfo(name = "before_text") val beforeText: String?,
    @ColumnInfo(name = "is_password") val isPassword: Boolean?
)


