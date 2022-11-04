package com.example.testing.utils

import android.content.Context
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Clicks::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clicksDao(): ClicksDao
}
