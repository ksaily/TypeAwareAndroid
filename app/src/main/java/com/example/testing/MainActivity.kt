package com.example.testing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.databinding.ActivitySignInBinding
import com.example.testing.fitbit.FitbitApiService
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.NonCancellable.start


class MainActivity : AppCompatActivity() {

    private lateinit var view: View
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)
        checkAccessibilityPermission()
        binding.FitbitBtn.setOnClickListener {
            Log.d("Thread", "Button clicked")
            Thread(Runnable {
                FitbitApiService.authorizeRequest()
            }).start()
        }

    }

    /** Check accessibility permissions again if not provided when returning to the app **/
    override fun onResume() {
        super.onResume()
        checkAccessibilityPermission()
    }


    /**Check for permissions **/
    private fun checkAccessibilityPermission(): Boolean {
        var accessEnabled = 0
        try {
            accessEnabled =
                Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        return if (accessEnabled == 0) {
            /** if access not granted, construct intent to request permission  */
            view.showSnackbar(view, getString(R.string.permission_required),
            Snackbar.LENGTH_INDEFINITE, "OK") {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                /** request permission via start activity for result  */
                startActivity(intent)
            }
            false
        } else {
            view.showSnackbar(view, getString(R.string.permission_granted),
            Snackbar.LENGTH_SHORT, null) {}
            true
        }
    }

    fun View.showSnackbar (
        view: View,
        msg: String,
        length: Int,
        actionMessage: CharSequence?,
        action: (View) -> Unit
    ) {
        val snackbar = Snackbar.make(view, msg, length)
        if (actionMessage != null) {
            snackbar.setAction(actionMessage) {
                action(this)
            }.show()
        } else {
            snackbar.show()
        }
    }
/**
    private var CLIENT_ID: String = "2393N9"
    private var REDIRECT_URL: Uri = Uri.parse("https://alertness-level-monitor.com")
    private lateinit var SECURE_KEY: String
    private val fitbitAuthUrl = "https://www.fitbit.com/oauth2/authorize"
    private val fitbitTokenUrl = "https://www.fitbit.com/oauth2/token"
    private val grantType = "client_credentials"

    var token: String? = null
    var tokenType: String? = null

    fun main() {
        val (_, _, result) = fitbitAuthUrl
            .httpPost(listOf("client_id" to CLIENT_ID,
                "response_type" to "token",
                "redirect_uri" to REDIRECT_URL,
                "expires_in" to "86400",
                "scope" to "sleep"))
            .responseString()
        println(result)
    }**/
}