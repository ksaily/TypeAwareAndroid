package com.example.testing.fitbit

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Auth(
    val access_token: String,
    val refresh_token: String,
    val scope: String
        )