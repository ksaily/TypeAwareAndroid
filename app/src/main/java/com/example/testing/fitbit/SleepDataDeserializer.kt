package com.example.testing.fitbit

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class SleepData(val sleep: List<SleepLog>)
data class SleepLog(val dateOfSleep: String, val duration: Long, val efficiency: Int)

class SleepDataDeserializer : ResponseDeserializable<SleepData> {
    override fun deserialize(content: String) = Gson().fromJson(content,
    SleepData::class.java)
    }