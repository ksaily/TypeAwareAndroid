package com.example.testing.fitbit

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class SleepDatav(val sleep: List<SleepLog>)
data class SleepLog(val dateOfSleep: String, val duration: Long, val efficiency: Int)

class SleepDataDeserializer : ResponseDeserializable<SleepDatav> {
    override fun deserialize(content: String) = Gson().fromJson(content,
    SleepDatav::class.java)
    }