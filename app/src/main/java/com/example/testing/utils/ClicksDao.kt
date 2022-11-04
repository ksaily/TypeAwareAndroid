package com.example.testing.utils

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClicksDao {
    @Query("SELECT * FROM clicks")
    fun getAll(): List<Clicks>

    @Query("SELECT * FROM clicks WHERE uid IN (:deviceIds)")
    fun loadAllByIds(deviceIds: IntArray): List<Clicks>

    @Insert
    fun insert(vararg clicks: Clicks)

    @Delete
    fun delete(vararg clicks: Clicks)

    @Update
    fun update(vararg clicks: Clicks)
}