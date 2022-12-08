package com.example.testing.fitbit
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.getCodeChallenge
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthConstants.CLIENT_SECRET
import com.github.scribejava.core.oauth.OAuth20Service
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Fitbit's OAuth2 client's implementation
 * source: https://dev.fitbit.com/docs/oauth2/
 */

class FitbitApiService {
    companion object {

        private val fitbitTokenUrl = "https://api.fitbit.com/oauth2/token"
        private val grantType = "authorization_code"
        var accessToken: String? = null
        var refreshToken: String? = null

        /**
         * Create an authorization request to send to Fitbit
         */
        fun authorizeRequestToken(code: String, state: String) {
            //val basicToken = "Basic " + Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
            val (request, response, result) = fitbitTokenUrl
                .httpPost(
                    listOf(
                        "client_id" to CLIENT_ID,
                        "code" to code,
                        "code_verifier" to CODE_VERIFIER,
                        "redirect_uri" to REDIRECT_URL,
                        "state" to state,
                        "grant_type" to grantType
                    )
                    //).appendHeader("Authorization", basicToken)
                ).responseString()
            println(request)
            println(result)
            when (result) {
                is Result.Success -> {
                    val jsonArray = JSONArray(response)
                    val jsonObject: JSONObject = jsonArray.getJSONObject(0)
                    accessToken = jsonObject.get("access_token") as String
                    refreshToken = jsonObject.get("refresh_token") as String
                    println(response)
                    Log.d("Authorization: ", "Access token: $accessToken")
                    Log.d("Authorization: ", "Refresh token: $refreshToken")
                    TODO("Check scopes and start requesting for user data")
                }
                is Result.Failure -> {
                    Log.d("HTTP request", response.toString())
                    TODO("If error is expired token, refresh token")
                }
            }
        }
    }
}