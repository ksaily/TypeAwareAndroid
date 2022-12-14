package com.example.testing.fitbit
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.testing.MainActivity
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.getCodeChallenge
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthConstants.CLIENT_SECRET
import com.github.scribejava.core.oauth.OAuth20Service
import com.google.gson.Gson
import com.google.gson.internal.Streams.parse
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.xml.sax.Parser
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.logging.Level.parse

/**
 * Fitbit's OAuth2 client's implementation
 * source: https://dev.fitbit.com/docs/oauth2/
 */

class FitbitApiService {
    companion object {

        private val fitbitTokenUrl = "https://api.fitbit.com/oauth2/token"
        private val grantType: String? = null
        var accessToken: String? = null
        var refreshToken: String? = null
        var date: String = "2022-12-12" //Date in the format YYYY-MM-DD
        var scope: String? = null
        var userId: String? = null
        var fitbitApiEndpoint = "https://api.fitbit.com"
        var runningThread: Boolean = true

        /**
         * Create an authorization request to send to Fitbit
         */
        fun authorizeRequestToken(code: String, state: String) {
            //val basicToken = "Basic " + Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
            val (request, response, result) = buildTokenRequest(
                "authorization_code", code, state)
            Log.d("HTTP Result", "Result: $result")
            Log.d("HTTP Response", "Result: $response")
            val (auth, err) = result
            val jsonObject = JSONTokener(auth).nextValue() as JSONObject
            when (result) {
                is Result.Success -> {
                    val (auth, err) = result
                    val jsonObject = JSONTokener(auth).nextValue() as JSONObject
                    accessToken = jsonObject.getString("access_token")
                    refreshToken = jsonObject.getString("refresh_token")
                    println(response)
                    Log.d("Authorization: ", "Access token: $accessToken")
                    Log.d("Authorization: ", "Refresh token: $refreshToken")
                }
                is Result.Failure -> {
                    val errObject = JSONTokener(err.toString()).nextValue() as JSONObject
                    val error = errObject.getString("errorType")
                    Log.d("HTTP request", response.toString())
                    try {
                        if (error == "expired_token") {
                            val (_,refreshResponse, refreshResult) = buildTokenRequest(
                                "refresh_token", code, state)
                            val (refresh, err) = refreshResult
                            when (refreshResult) {
                                is Result.Success -> {
                                    accessToken = jsonObject.getString("access_token")
                                    refreshToken = jsonObject.getString("refresh_token")
                                    userId = jsonObject.getString("user_id")
                                    Log.d("Authorization", "Access token updated")
                                }
                                is Result.Failure -> {
                                    Log.d("Authorization", "Error, Access token not updated")
                                }
                            }
                        } else {
                            Log.d("Authorization", "Redirecting user back to main screen")
                        }
                    } catch (e: Exception) {
                        Log.d("Authorization", "Error: $e")
                    }
                }
            }
        }

        fun getSleepData(date: String) {
                FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
                val url = "/sleep/date/$date.json"

                val (request, response, result) = url.httpGet().header(
                    "Authorization" to
                            "Bearer $accessToken"
                ).responseString()
            //.responseObject(SleepDataDeserializer())
            Log.d("SleepData", "Request: $request")
                val (sleepData, error) = result
                if (error == null) {
                    //Print the sleep data
                    val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                    val jsonArray = jsonObject.getJSONArray("sleep")
                    val duration = jsonArray.getJSONObject(0).getString("duration")
                    val dateOfSleep = jsonArray.getJSONObject(0).getString("dateOfSleep")
                    Log.i("Sleep", "Duration: $duration")
                    Log.i("Sleep", "Date of sleep : $dateOfSleep")
                    runningThread = false
                }
            else {
                println(error)
                    runningThread = false
            }
        }

        fun buildTokenRequest(
            grant: String,
            code: String,
            state: String
        ): Triple<Request, Response, Result<String, Exception>> {
            return if (grant == "authorization_code") {
                fitbitTokenUrl.httpPost(listOf(
                    "client_id" to CLIENT_ID,
                    "code" to code,
                    "code_verifier" to CODE_VERIFIER,
                    "redirect_uri" to REDIRECT_URL,
                    "state" to state,
                    "grant_type" to grant
                )).responseString()
            } else {
                Log.d("Authorization", "Refresh token needed")
                fitbitTokenUrl.httpPost(listOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken
                )).responseString()
            }
        }

    }
}