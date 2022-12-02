package com.example.testing.fitbit
import android.net.Uri
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import android.util.Base64
import java.security.SecureRandom

/**
 * Fitbit's OAuth2 client's implementation
 * source: https://dev.fitbit.com/docs/oauth2/
 */

class FitbitApiService {
    companion object {
        private var CLIENT_ID: String = "2393N9"
        private var REDIRECT_URL: Uri = Uri.parse("https://alertness-level-monitor.com")
        private lateinit var SECURE_KEY: String
        private val fitbitAuthUrl = "https://www.fitbit.com/oauth2/authorize"
        private val fitbitTokenUrl = "https://www.fitbit.com/oauth2/token"
        private val grantType = "client_credentials"

        var token: String? = null
        var tokenType: String? = null

        fun authorizeRequest() {
            val (_, _, result) = fitbitAuthUrl
                .httpPost(
                    listOf(
                        "client_id" to CLIENT_ID,
                        "response_type" to "token",
                        "redirect_uri" to REDIRECT_URL,
                        "expires_in" to "86400",
                        "scope" to "sleep"
                    )
                )
                .responseString()
            println(result)
        }
    }

}