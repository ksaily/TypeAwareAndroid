package com.example.testing.fitbit

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.getCodeChallenge
import com.example.testing.ui.data.SleepData
import com.example.testing.utils.Utils
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level.parse
import kotlin.coroutines.coroutineContext

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
        var fitbitPermission: Boolean = false

        /**
         * Create an authorization request to send to Fitbit
         */
        fun authorizeRequestToken(code: String, state: String) {
            //val basicToken = "Basic " + Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
            val (request, response, result) = buildTokenRequest(
                "authorization_code", code, state)
            Log.d("HTTP Result", "Result: $result")
            Log.d("HTTP Response", "Result: $response")
            try {
                val (auth, err) = result
                val jsonObject = JSONTokener(auth).nextValue() as JSONObject
                when (result) {
                    is Result.Success -> {
                        //val (auth, err) = result
                        //val jsonObject = JSONTokener(auth).nextValue() as JSONObject
                        accessToken = jsonObject.getString("access_token")
                        refreshToken = jsonObject.getString("refresh_token")
                        println(response)
                        Log.d("Authorization: ", "Access token: $accessToken")
                        Utils.saveSharedSetting(Graph.appContext,
                        "access_token", accessToken)
                        Utils.saveSharedSetting(Graph.appContext,
                        "refresh_token", refreshToken)
                        Utils.saveSharedSettingBoolean(Graph.appContext,
                        "loggedInFitbit", true)
                        Log.d("Authorization: ", "Refresh token: $refreshToken")
                        fitbitPermission = true
                    }
                    is Result.Failure -> {
                        val errObject = JSONTokener(err.toString()).nextValue() as JSONObject
                        val error = errObject.getString("errorType")
                        Log.d("HTTP request", response.toString())
                            if (error == "expired_token") {
                                val (_, refreshResponse, refreshResult) = buildTokenRequest(
                                    "refresh_token", code, state)
                                val (refresh, err) = refreshResult
                                when (refreshResult) {
                                    is Result.Success -> {
                                        accessToken = jsonObject.getString("access_token")
                                        refreshToken = jsonObject.getString("refresh_token")
                                        userId = jsonObject.getString("user_id")
                                        Log.d("Authorization", "Access token updated")
                                        Utils.saveSharedSetting(Graph.appContext,
                                            "access_token", accessToken)
                                        Utils.saveSharedSetting(Graph.appContext,
                                            "refresh_token", refreshToken)
                                        Utils.saveSharedSettingBoolean(Graph.appContext,
                                            "loggedInFitbit", true)
                                        fitbitPermission = true
                                    }
                                    is Result.Failure -> {
                                        Log.d("Authorization", "Error, Access token not updated")
                                        Utils.saveSharedSettingBoolean(Graph.appContext,
                                        "loggedInFitbit", false)
                                        fitbitPermission = false
                                    }
                                }
                            } else {
                                Log.d("Authorization", "Redirecting user back to main screen")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.d("Fitbit Authorization", "Error: $e")
            }
        }

        fun getSleepData(date: String): SleepData {
            try {
                FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
                val url = "/sleep/date/$date.json"

                val (_, response, result) = url.httpGet().header(
                    "Authorization" to
                            "Bearer $accessToken"
                ).responseString()
                //.responseObject(SleepDataDeserializer())
                Log.d("SleepData", "Response: $response")
                val (sleepData, error) = result
                Log.d("GetSleepData", "Error: $error")
                val errObject = JSONTokener(error.toString()).nextValue() as JSONObject
                Log.d("GetSleepData", "ErrorObject: $errObject")
                val err = errObject.getString("errorType")
                Log.d("GetSleepData", "ErrorType: $err")
                if (err == null) {
                    Utils.saveSharedSettingBoolean(Graph.appContext, "loggedInFitbit", true)
                    //Print the sleep data
                    val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                    val jsonArray = jsonObject.getJSONArray("sleep")
                    val summary = jsonObject.getJSONObject("summary")
                    val minutesAsleep = summary.getInt("totalMinutesAsleep")
                    var endDateTime = jsonArray.getJSONObject(0).getString("endTime")
                    var startDateTime = jsonArray.getJSONObject(0).getString("startTime")
                    var startTime = convertISOTimeToTime(startDateTime)
                    var endTime = convertISOTimeToTime(endDateTime)
                    val duration = jsonArray.getJSONObject(0).getString("duration")
                    val dateOfSleep = jsonArray.getJSONObject(0).getString("dateOfSleep")
                    Log.i("Sleep", "Duration: $duration")
                    Log.i("Sleep", "Date of sleep : $dateOfSleep")
                    runningThread = false
                    return SleepData(true, minutesAsleep, startTime, endTime)
                } else {
                    var code = Utils.readSharedSettingString(
                        Graph.appContext,
                        "authorization_code", ""
                    )
                    var state = Utils.readSharedSettingString(
                        Graph.appContext, "state", ""
                    )
                    return if (code != null && state != null) {
                        authorizeRequestToken(code, state)
                        getSleepData(date)
                    } else
                        SleepData(false, 0, "-", "-",)
                }
            } catch (e: Exception) {
                Log.d("Fitbit Authorization", "Error: $e")
                return SleepData(false, 0, "-", "-",)
            }
        }

        fun buildTokenRequest(
            grant: String,
            code: String,
            state: String,
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

        fun convertISOTimeToTime(isoTime: String): String? {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
            var convertedDate: Date? = null
            var formattedDate: String? = null
            try {
                convertedDate = sdf.parse(isoTime)
                formattedDate = SimpleDateFormat("HH:mm").format(convertedDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            return formattedDate
        }

    }
}