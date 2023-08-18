package com.example.testing.fitbit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.lifecycle.lifecycleScope
import java.util.UUID
import com.example.testing.fitbit.CodeChallenge.Companion.CLIENT_ID
import com.example.testing.fitbit.CodeChallenge.Companion.REDIRECT_URL
import com.example.testing.fitbit.CodeChallenge.Companion.CODE_VERIFIER
import com.example.testing.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val builder = CustomTabsIntent.Builder()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val authorizationUrl = buildUrl(fitbitAuthUrl)
            launchAuthorizationPage(authorizationUrl)
        }
        catch (e: Exception) {
            Log.d("Error", "$e")
        }
    }

        /**

        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER,
            Uri.parse("android-app://" + Graph.appContext.packageName))
        Log.d("Authorization", "Activity started")
        try {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d("Authorization", "$authorizationUrl")
            customTabsIntent.launchUrl(this, Uri.parse(authorizationUrl.toString()))
        }**/

    private fun launchAuthorizationPage(uri: Uri) {

        val intent = Intent(Intent.ACTION_VIEW, uri)
        //intent.putExtra(Intent.EXTRA_REFERRER,
        //      Uri.parse("android-app://" + Graph.appContext.packageName))
        startActivity(intent)
    }


    /**

    override fun onResume() {
        super.onResume()

        // Step 3: Handle the Redirect
        val uri = intent.data
        Log.d("Auth", uri.toString())
        if (uri != null && uri.toString().startsWith(REDIRECT_URL)) {
            handleRedirect(uri)
        }
        else {
            finish()
            return
        }
    }**/

    private fun handleRedirect(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        if (code != null && state != null) {
            Utils.saveSharedSetting("authorization_code", code)
            Utils.saveSharedSetting("state", state)
            AUTH_CODE = code
            uniqueState = state
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    FitbitApiService.authorizeRequestToken(code, state)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            finish()
            return
        } else {
            //No authorization code received, return to MainActivity
            Log.d("Authorization", "Authorization code not received")
            finish()
            return
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data
        if (uri != null && uri.toString().startsWith(REDIRECT_URL)) {
            handleRedirect(uri)
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        return
    }

    private fun buildUrl(url: String): Uri {
        uniqueState = UUID.randomUUID().toString()
        val uri = Uri.parse(url)
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("code_challenge", CodeChallenge.getCodeChallenge(CODE_VERIFIER))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("scope", "sleep")
            .appendQueryParameter("prompt", "login")
            .appendQueryParameter("state", uniqueState)

        return uri.build()
    }
}
