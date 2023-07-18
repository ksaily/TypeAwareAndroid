package com.example.testing

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.fitbit.FitbitApiService
import com.example.testing.notifications.AlarmReceiver
import com.example.testing.questionnaire.DailyQuestionnaireDialog
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.onboarding.*
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.checkPermissions
import com.example.testing.utils.Utils.Companion.currentDate
import com.example.testing.utils.Utils.Companion.getCurrentDateString
import com.example.testing.utils.Utils.Companion.getSharedPrefs
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import com.example.testing.utils.Utils.Companion.readSharedSettingString
import com.example.testing.utils.Utils.Companion.saveSharedSetting
import com.example.testing.utils.Utils.Companion.saveSharedSettingBoolean
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    private lateinit var view: View
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNav: BottomNavigationView
    private var currentAlertDialog: AlertDialog? = null
    private val homeFragment = HomeFragment()
    private val chartFragment = ChartFragment()
    private val settingsFragment = SettingsFragment()
    private val questionnaireDialog = DailyQuestionnaireDialog()
    val database = Firebase.database



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                saveSharedSetting("firebase_auth_uid", uid)
                Log.d("FirebaseAuth", "UID $uid")
            }
            .addOnFailureListener { exception ->
                Log.d("Error", exception.toString())
            }

        bottomNav = binding.bottomNav
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment -> {
                    Log.d("BottomNav", "homefragment selected")
                    loadFragment(this, homeFragment, null, "homeFragment", false)
                }
                R.id.settingsFragment -> {
                    Log.d("BottomNav", "settingfragment selected")
                    loadFragment(this, settingsFragment, null, "settingsFragment", false)
                }
                R.id.chartFragment -> {
                    Log.d("BottomNav", "chartfragment selected")
                    loadFragment(this, chartFragment, null, "chartFragment", false)
                }
            }
            true
        }
        checkFirstLogin()
        //bottomNav.selectedItemId = R.id.homeFragment

        getSharedPrefs().registerOnSharedPreferenceChangeListener(this)
        /**if (readSharedSettingBoolean(
                "permissions_granted", false)) {
            // Don't show questionnaire if it is the first day
            questionnaireDialog.showQuestionnaire(
                currentDate == readSharedSettingString(
                getString(R.string.sharedpref_questionnaire_day), ""
            ))
        }**/
        AlarmReceiver.scheduleNotification(Graph.appContext)
    }

    private fun checkDailyQuestionnaire() {
        println(!readSharedSettingBoolean(
            getString(R.string.sharedpref_questionnaire_ans), false))
        println(questionnaireDialog.isVisible)
        println(getCurrentDateString() != readSharedSettingString(getString(R.string.sharedpref_questionnaire_day), ""))
        if (readSharedSettingBoolean(getString(R.string.sharedpref_first_login), false) &&
            !readSharedSettingBoolean(
                getString(R.string.sharedpref_questionnaire_ans), false) &&
            !questionnaireDialog.isVisible &&
            getCurrentDateString() != readSharedSettingString(getString(R.string.sharedpref_questionnaire_day), "")
        ) {
            if (!readSharedSettingBoolean(getString(R.string.sharedpref_firstweek_done), false)) {
            //Check that user info is given before showing questionnaire
            Log.d("DailyQuestionnaire", "Questionnaire not answered")
            questionnaireDialog.show(supportFragmentManager, "DailyQuestionnaireDialog")
            }
            else if (!readSharedSettingBoolean(
                    getString(R.string.sharedpref_questionnaire_ans), false)) {
                Log.d("Second week", "started")
                binding.secondWeekQstnrBtn.isVisible = true
                binding.secondWeekQstnrBtn.setOnClickListener {
                    if (!readSharedSettingBoolean(getString(R.string.sharedpref_questionnaire_ans), false)) {
                        questionnaireDialog.show(supportFragmentManager, "DailyQuestionnaireDialog")
                    }
                    else {
                        Toast.makeText(this, "You have already answered today's questionnaire",
                        Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Check accessibility permissions & battery optimization
     *  again if not provided when returning to the app
     **/
    override fun onResume() {
        super.onResume()
        checkFirstLogin()
        checkPermissions(applicationContext)
        afterPermissionsReceived()
        checkDailyQuestionnaire()
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        Log.d("SharedPref", "OnSharedPreferenceChanged")
        checkFirstLogin()


        if (key == getString(R.string.sharedpref_questionnaire_ans)) {
            Log.d("DailyQuestionnaire", "key changed")
            checkDailyQuestionnaire()
        }
        if (key == getString(R.string.sharedpref_firstweek_done) &&
                readSharedSettingBoolean(getString(R.string.sharedpref_firstweek_done), false)) {
            questionnaireDialog.changeToSecondWeek()
        }

        if (key=="study_finished" && readSharedSettingBoolean("study_finished", false)) {
            Log.d("Study finished", "true")
            questionnaireDialog.show(supportFragmentManager, "DailyQuestionnaireDialog")
            }
        /**else if (key == "accessibility_permission" && !readSharedSettingBoolean(
                Graph.appContext, "accessibility_permission", false)
            && readSharedSettingBoolean(Graph.appContext,
                "first_login_done", false)
        ) {
            Log.d("Timer", "check permissions")
            checkPermissions(applicationContext)
        }**/
    }

    private fun afterPermissionsReceived() {
        if (readSharedSettingBoolean(getString(R.string.sharedpref_permissions), false)
        ) { questionnaireDialog.showQuestionnaire(
            Utils.getCurrentDateString() == readSharedSettingString(getString(R.string.sharedpref_questionnaire_day), ""
            )) }
    }


    private fun checkFirstLogin() {
        if (!readSharedSettingBoolean(getString(R.string.sharedpref_first_login), false)
        ) {
            onFirstLogin()
        } else {
            afterFirstLoginDone()
        }
    }

    private fun onFirstLogin() {
        if (!readSharedSettingBoolean(getString(R.string.sharedpref_consent), false)
        ) {
            Log.d("MainActivity", "Start consent fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, ConsentFragment(), null,
                "consentFragment", true)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(getString(R.string.sharedpref_user_info), false)
        ) {
            Log.d("MainActivity", "Start userinfo fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, UserInfoFragment(), null,
                "userInfoFragment", true)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(getString(R.string.sharedpref_onboarding), false)
        ) {
            Log.d("MainActivity", "Start onboarding activity")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, OnboardingFragment(), null,
            "onboardingFragment", true)
            bottomNav.isVisible = false
        }
        else {
            Log.d("MainActivity", "First login done")
            val userId = readSharedSettingString(getString(R.string.sharedpref_userid), "").toString()
            // Set user id for crash reports
            FirebaseCrashlytics.getInstance().setUserId(
                userId
            )
            if (!supportFragmentManager.isStateSaved) {
                Log.d("LoadFragment", "home")
                loadFragment(this, homeFragment, null, "homeFragment", true)
            }
            saveSharedSetting(getString(R.string.sharedpref_questionnaire_day), currentDate)
            saveSharedSettingBoolean(getString(R.string.sharedpref_first_login), true)
        }
    }

    private fun afterFirstLoginDone() {
        Log.d("MainActivity", "After first login done")
        //if (!homeFragment.isVisible) {
            //loadFragment(this, homeFragment, null, "homeFragment", false)
          //  bottomNav.isVisible = true
        //}
        bottomNav.isVisible = true
        if (!supportFragmentManager.isStateSaved) {
            //Check which fragment is visible
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currentFragment == null) {
                Log.d("LoadFragment", "home")
                loadFragment(this, homeFragment, null, "homeFragment", true)
            }
        }
        checkPermissions()
        questionnaireDialog.showQuestionnaire(
            currentDate == readSharedSettingString(getString(R.string.sharedpref_questionnaire_day), ""
            ))

        if (!readSharedSettingBoolean(getString(
                R.string.sharedpref_accessibility), false)) {
            Log.d("afterFirstLoginDone", "check accessibility")
            showAlertDialog(this@MainActivity, 0)
        }
        if (!readSharedSettingBoolean(getString(R.string.sharedpref_battery), false)) {
            Log.d("afterFirstLoginDone", "check battery")
            showAlertDialog(this@MainActivity, 1)
        }
    }

    /**
     * @param action 0 for accessibility settings, 1 for battery optimization settings
     */
    private fun showAlertDialog(context: Context, action: Int) {
        if (currentAlertDialog != null && currentAlertDialog!!.isShowing) {
            return
        }
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)
        when (action) {
            0 -> {
                alertDialog.setTitle(R.string.accessibility_perm_snackbar_title)
                alertDialog.setMessage(R.string.accessibility_perm_snackbar_msg)
            }
            1 -> {
                alertDialog.setTitle(R.string.battery_opt_snackbar_title)
                alertDialog.setMessage(R.string.battery_opt_snackbar_msg)
            }
        }
        alertDialog.setPositiveButton(
            "OK"
        ) { _, _ ->
            when (action) {
                0 -> {
                    Utils.checkAccessibilityPermission(Graph.appContext, true)
                }

                1 -> {
                    Utils.checkBattery(Graph.appContext)

                }
            }

        }
        alertDialog.setNegativeButton(
            "Cancel"
        ) { _, _ ->
        }
        currentAlertDialog = alertDialog.create()
        currentAlertDialog?.setCanceledOnTouchOutside(true)
        currentAlertDialog?.show()
    }
}
