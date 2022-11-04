package com.example.testing.utils

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class DatabaseHelper {

    private fun createDatabase(context: Context) {
        var db = Room.databaseBuilder(context, AppDatabase::class.java, "Clicks"
        ).build()
    }
}