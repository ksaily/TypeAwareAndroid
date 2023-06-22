package com.example.testing.fitbit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.MainActivity
import java.net.URI
import java.net.URL
import java.util.UUID
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
import com.example.testing.fitbit.FitbitApiService.Companion.runningThread
import com.example.testing.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64

/**
 * This activity will launch the login screen to Fitbit and
 * handle the redirect that contains authorization code.
 * It will then make a request to the Fitbit API to retrieve the access token for this account
 * and pass the token back to the MainActivity
 */

class AuthenticationActivity : AppCompatActivity() {
    private val fitbitAuthUrl = "https://www.fitbit.com/oauth2/authorize"
    private var uniqueState: String? = null
    private var AUTH_CODE: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authorizationUrl = buildUrl(fitbitAuthUrl)
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        intent.flags = FLAG_ACTIVITY_NEW_TASK
        //intent.putExtra(Intent.EXTRA_REFERRER,
        //    Uri.parse("android-app://" + Graph.appContext.packageName))
        Log.d("Authorization", "Activity started")
        try {
            Log.d("Authorization", "$authorizationUrl")
            customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl))
        } catch (e: Exception) {
            Log.d("Error", "$e")
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Authorization", "On new intent")
        val code = intent?.data?.getQueryParameter("code")
        val state = intent?.data?.getQueryParameter("state")
        //val redirect = intent?.data?.getQueryParameter("redirect")
        if (code != null && state != null) {
            Log.d("Authorization", "Authorization code is: $code")
            Log.d("Authorization", "Authorization state is: $state")
            Log.d("Authorization", "Redirect uri is $intent")
            Utils.saveSharedSetting(Graph.appContext,
            "authorization_code", code)
            Utils.saveSharedSetting(Graph.appContext,
                "state", state)
            AUTH_CODE = code
            uniqueState = state
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    FitbitApiService.authorizeRequestToken(code, state)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }/**
            Thread(Runnable {
                try {
                    if (!runningThread) {
                        return@Runnable
                    }
                    FitbitApiService.authorizeRequestToken(code, state)
                    //FitbitApiService.getSleepData("2022-12-10")
                    //Utils.saveSharedSettingBoolean(Graph.appContext, "loggedInFitbit", true)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }).start()**/
            val returnIntent = Intent(this, MainActivity::class.java)
            startActivity(returnIntent)
        } else {
            //No authorization code received, return to MainActivity
            Log.d("Authorization", "Authorization code not received")
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        finish()
        return
    }

    private fun buildUrl(url: String): String {
        uniqueState = UUID.randomUUID().toString()
            val uri = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", CodeChallenge.CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .appendQueryParameter("code_challenge", CodeChallenge.getCodeChallenge(CODE_VERIFIER))
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("scope", "sleep")
                .appendQueryParameter("state", uniqueState)
                .build()
        return uri.toString()
    }
}