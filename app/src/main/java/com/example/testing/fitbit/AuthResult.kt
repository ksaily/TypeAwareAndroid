package com.example.testing.fitbit

data class AuthResult(
    var authorization_code: String? = null,
    var access_token : String? = null,
    var user_id: String? = null,
    var token_type: String? = null,
    var expires_in : Number,
    var scope: List<String?>
)