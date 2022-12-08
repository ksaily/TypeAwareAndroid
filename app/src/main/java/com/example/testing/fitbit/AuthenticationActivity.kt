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
import java.net.URI
import java.net.URL
import java.util.UUID
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
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
        Log.d("Authorization", "Activity started")
        try {
            Log.d("Authorization", "$authorizationUrl")
            customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl))
        } catch (e: Exception) {
            Log.d("Error", "$e")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("Authorization", "On new intent")
        val code = intent?.data?.getQueryParameter("code")
        val state = intent?.data?.getQueryParameter("state")
        val redirect = intent?.data?.getQueryParameter("r")
        if (code != null && state != null) {
            Log.d("Authorization", "Authorization code is: $code")
            Log.d("Authorization", "Authorization state is: $state")
            Log.d("Authorization", "Redirect uri is $intent")
            AUTH_CODE = code
            uniqueState = state
            Thread(Runnable {
                FitbitApiService.authorizeRequestToken(code, state)
            }).start()
        } else {
            Log.d("Authorization", "Authorization code not received")
            TODO("Handle error")
        }
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