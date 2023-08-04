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
import com.example.testing.data.SleepData
import com.example.testing.utils.Utils
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.json.JSONTokener
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

/**
 * Fitbit's OAuth2 client's implementation
 * source: https://dev.fitbit.com/docs/oauth2/
 */

class FitbitApiService {
    companion object {

        private const val fitbitTokenUrl = "https://api.fitbit.com/oauth2/token"
        private val grantType: String? = null
        var accessToken: String? = null
        var refreshToken: String? = null
        var scope: String? = null
        var userId: String? = null
        var fitbitApiEndpoint = "https://api.fitbit.com"
        var runningThread: Boolean = true
        var fitbitPermission: Boolean = false
        var authAttempted: Boolean = false


        /**
         * Create an authorization request to send to Fitbit
         */
        fun authorizeRequestToken(code: String, state: String) {
            try {
                //val basicToken = "Basic " + Base64.getEncoder().encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray())
                val (request, response, result) = buildTokenRequest(
                    "authorization_code", code, state)
                val header = request.headers
                val (auth, err) = result
                when (result) {
                    is Result.Success -> {
                        val jsonObject = JSONTokener(auth).nextValue() as JSONObject
                        accessToken = jsonObject.getString("access_token")
                        refreshToken = jsonObject.getString("refresh_token")
                        Utils.saveSharedSetting("access_token", accessToken)
                        Utils.saveSharedSetting("refresh_token", refreshToken)
                        Utils.saveSharedSettingBoolean("loggedInFitbit", true)
                        fitbitPermission = true
                    }
                    is Result.Failure -> {
                        if (response.statusCode == 401) {
                            // Get refresh token
                            getRefreshToken(code, state)
                        } else {
                            // Handle other types of failure
                            Utils.saveSharedSettingBoolean("loggedInFitbit",
                                false)
                            Log.d("Authorization", "Redirecting user back to main screen")
                        }
                    }
                }
            } catch (e: Exception) {
                Utils.saveSharedSettingBoolean("loggedInFitbit", false)
                Log.d("Authorization", "Error: $e")
            }
        }

        fun getRefreshToken(code: String, state: String) {
            val (request, response, result) = buildTokenRequest(
                "refresh_token", code, state
            )

            when (result) {
                is Result.Success -> {
                    val jsonObject = JSONTokener(result.get()).nextValue() as JSONObject
                    accessToken = jsonObject.getString("access_token")
                    refreshToken = jsonObject.getString("refresh_token")
                    userId = jsonObject.getString("user_id")
                    Utils.saveSharedSetting("access_token", accessToken)
                    Utils.saveSharedSetting("refresh_token", refreshToken)
                    Utils.saveSharedSettingBoolean("loggedInFitbit", true)
                    fitbitPermission = true
                }
                is Result.Failure -> {
                    Utils.saveSharedSettingBoolean("loggedInFitbit", false)
                    Log.d("Authorization", "Error, Access token not updated")
                    fitbitPermission = false
                }
            }
        }


        fun getSleepData(date: String): SleepData {
                try {
                    accessToken = Utils.readSharedSettingString("access_token", "")
                    FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
                    val url = "/sleep/date/$date.json"

                    val (_, response, result) = url.httpGet()
                        .header(
                    "Authorization" to
                                "Bearer $accessToken" )
                        .responseString()
                    val (sleepData, error) = result
                    val endTime: String?
                    val startTime: String?
                    val minutesAsleep: Int?
                    if (response.isSuccessful) {
                        val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                        val jsonArray = jsonObject.getJSONArray("sleep")
                        val summary = jsonObject.getJSONObject("summary")
                        minutesAsleep = summary.getInt("totalMinutesAsleep")
                        val endDateTime = jsonArray.getJSONObject(0).getString("endTime")
                        val startDateTime = jsonArray.getJSONObject(0).getString("startTime")
                        startTime = convertISOTimeToTime(startDateTime)
                        endTime = convertISOTimeToTime(endDateTime)
                        val duration = jsonArray.getJSONObject(0).getString("duration")
                        val dateOfSleep = jsonArray.getJSONObject(0).getString("dateOfSleep")
                        runningThread = false
                        authAttempted = false
                        val stringJson = jsonObject.toString(2)
                        val jsonMap: Map<String, Any> = Gson().fromJson(stringJson, object : TypeToken<HashMap<String, Any>>() {}.type)
                        return SleepData(true, minutesAsleep, startTime, endTime, jsonMap)
                    } else if (response.statusCode == 401) {
                        // Get refresh token
                        val code = Utils.readSharedSettingString("authorization_code", "")
                        val state = Utils.readSharedSettingString("state", "")
                        return if (code!!.isNotEmpty() && state!!.isNotEmpty() && !authAttempted) {
                            Log.d("GetSleepDataFailure", "Re-authorizing")
                            getRefreshToken(code, state)
                            authAttempted = true
                            getSleepData(date)
                        } else {
                            authAttempted = false
                            SleepData(false, 0, "-", "-", HashMap<String, Any>())
                        }
                    } else {
                        authAttempted = false
                        return SleepData(false, 0, "-", "-", HashMap<String, Any>())
                    }
                } catch (e: Exception) {
                    authAttempted = false
                    Log.d("Fitbit Authorization(Get Sleep Data)", "Error: $e")
                    return SleepData(false, 0, "-", "-", HashMap<String, Any>() )
                }
        }

        fun buildTokenRequest(
            grant: String,
            code: String,
            state: String,
        ): Triple<Request, Response, Result<String, Exception>> {
            //val encdodedStr = CodeChallenge.getHeader()
            return if (grant == "authorization_code") {
                fitbitTokenUrl.httpPost(listOf(
                    "client_id" to CLIENT_ID,
                    "grant_type" to "authorization_code",
                    "code" to code,
                    "code_verifier" to CODE_VERIFIER,
                    "redirect_uri" to REDIRECT_URL))
                    .header("Content-Type" to "application/x-www-form-urlencoded").responseString()
            } else {
                refreshToken = Utils.readSharedSettingString("refresh_token", "")
                Log.d("Authorization", "Refresh token needed")
                fitbitTokenUrl.httpPost(listOf(
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken,
                    "client_id" to CLIENT_ID
                )).responseString()
            }
        }

        private fun convertISOTimeToTime(isoTime: String): String? {
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