package com.example.testing

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.notifications.AlarmReceiver
import com.example.testing.questionnaire.DailyQuestionnaireDialog
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.onboarding.*
import com.example.testing.ui.viewmodel.DailyQuestionnaireViewModel
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.checkPermissions
import com.example.testing.utils.Utils.Companion.currentDate
import com.example.testing.utils.Utils.Companion.dateHasPassed
import com.example.testing.utils.Utils.Companion.getSharedPrefs
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import com.example.testing.utils.Utils.Companion.readSharedSettingString
import com.example.testing.utils.Utils.Companion.saveSharedSetting
import com.example.testing.utils.Utils.Companion.saveSharedSettingBoolean
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

/**
 * First week, show only survey for the user: How do you think you did this week
 * Second week, survey first and then reveal them the data
 * Q1: How many words did you type during date x?
 * Q2: Were you generally typing faster than normally
 * (Compare the participant's performance to others, how they feel about it)
 * Q2: How often did you have to correct your typing? (Scale 1-7)
 * Q3: At what time of day were you most active with typing? (some kind of selector)
 *
 */
private val alarmManager = Graph.appContext.getSystemService(ALARM_SERVICE) as AlarmManager
private val alarmPendingIntent by lazy {
    val intent = Intent(Graph.appContext, AlarmReceiver::class.java)
    PendingIntent.getBroadcast(Graph.appContext, 0, intent, 0)
}
private const val HOUR_TO_SHOW_PUSH = 18
class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    private lateinit var view: View
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var handler: Handler
    private lateinit var firebaseAnalytics: FirebaseAnalytics


    // Chart variables:
    private val MAX_X_VALUE = 13
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Orders"
    private val GROUP_2_LABEL = ""
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.8f
    private var chart: BarChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Orders", "Inventory"
    )
    private val calendar: Calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private val homeFragment = HomeFragment()
    private val chartFragment = ChartFragment()
    private val settingsFragment = SettingsFragment()
    //private val viewModel: DailyQuestionnaireViewModel by viewModels()
    private val questionnaireDialog = DailyQuestionnaireDialog()
    val database = Firebase.database("https://health-app-9c151-default-rtdb.europe-west1.firebasedatabase.app")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        val sharedPrefs = getSharedPrefs()
        //val transaction = supportFragmentManager.beginTransaction()
            //Utils.checkBattery(applicationContext)
            /**
            if (!readSharedSettingBoolean(applicationContext,
                    "consent_given",
                    false)) {
                loadFragment(this, ConsentFragment(), null,
                    "consentFragment", false)
            }**/



        //loadFragment(this, homeFragment, null, "homeFragment", true)
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

        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
        if (readSharedSettingBoolean(Graph.appContext,
                "permissions_granted", false)) {
            // Don't show questionnaire if it is the first day
            questionnaireDialog.showQuestionnaire(currentDate == readSharedSettingString(
                Graph.appContext, getString(R.string.sharedpref_questionnaire_day), ""
            ))
        }
        //Utils.checkPermissions(applicationContext)
    }

    private fun checkDailyQuestionnaire() {
        if (!Utils.readSharedSettingBoolean(Graph.appContext,
                getString(R.string.sharedpref_questionnaire_ans), false) &&
            !questionnaireDialog.isAdded &&
                readSharedSettingBoolean(Graph.appContext,
                "first_login_done", false)
        ) {
            //Check that user info is given before showing questionnaire
            Log.d("DailyQuestionnaire", "Questionnaire not answered")
            questionnaireDialog.show(supportFragmentManager, "DailyQuestionnaireDialog")
        }
    }

    private fun checkDayOfQuestionnaire() {
        if (!getSharedPrefs().contains(getString(R.string.sharedpref_questionnaire_day))) {
            //Don't show questionnaire because it is the first day
            Utils.saveSharedSetting(Graph.appContext,
                getString(R.string.sharedpref_questionnaire_day), currentDate)
            val weekFromNow = Utils.getDateWeekFromNow(currentDate)
            saveSharedSetting(Graph.appContext,
            getString(R.string.sharedpref_firstweek_end_date), weekFromNow)
        }  else {
            if (!readSharedSettingBoolean(Graph.appContext, getString(R.string.sharedpref_firstweek_done)
                    , false) &&
                dateHasPassed(readSharedSettingString(Graph.appContext,
                    getString(R.string.sharedpref_firstweek_end_date), "").toString())) {
                Log.d("Questionnaire", "First day")
                //Check if date has passed to check how many questionnaires have been answered
                Utils.checkQuestionnaireWeek(readSharedSettingString(Graph.appContext,
                    "questionnaire_first_day", "").toString(), 1)
            }
            else if (readSharedSettingBoolean(Graph.appContext, getString(R.string.sharedpref_firstweek_done)
                    , false) &&
                dateHasPassed(readSharedSettingString(Graph.appContext,
                    getString(R.string.sharedpref_firstweek_end_date), "").toString())) {
                // Second week
                Utils.checkQuestionnaireWeek(readSharedSettingString(Graph.appContext,
                    getString(R.string.sharedpref_secondweek_start_date), "").toString(), 2)
                binding.secondWeekQstnrBtn.isVisible = true
                binding.secondWeekQstnrBtn.setOnClickListener {
                    questionnaireDialog.show(supportFragmentManager, "DailyQuestionnaireDialog")
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
        //Utils.checkPermissions(applicationContext)
        /**
        if (readSharedSettingBoolean(Graph.appContext,
                "permissions_granted", false)) {
            questionnaireDialog.showQuestionnaire()
            checkDailyQuestionnaire()
        }**/
    }


    override fun onDestroy() {
        super.onDestroy()
        getSharedPrefs().unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        /**
        if (!readSharedSettingBoolean(Graph.appContext,
                "accessibility_permission", false)
        ) {
            Log.d("CheckPref", "Accessibility")
            Utils.showAlertDialog(this@MainActivity, 0)
        }
        if (!readSharedSettingBoolean(Graph.appContext,
                "accessibility_permission", false)
        ) {
            Log.d("CheckPref", "Battery")
            Utils.showAlertDialog(this@MainActivity, 1)
        }**/
        checkFirstLogin()
        Log.d("SharedPref", "OnSharedPreferenceChanged")

        if (key == getString(R.string.sharedpref_permissions) && readSharedSettingBoolean(
                Graph.appContext, getString(R.string.sharedpref_permissions), false)
        ) { questionnaireDialog.showQuestionnaire(currentDate == readSharedSettingString(
            Graph.appContext, getString(R.string.sharedpref_questionnaire_day), ""
        )) }

        else if (key == getString(R.string.sharedpref_questionnaire_ans)) {
            Log.d("DailyQuestionnaire", "key changed")
            checkDailyQuestionnaire()
        } else if (key == "accessibility_permission" && !readSharedSettingBoolean(
                Graph.appContext, "accessibility_permission", false)
            && readSharedSettingBoolean(Graph.appContext,
                "first_login_done", false)
        ) {
            Log.d("Timer", "check permissions")
            checkPermissions(applicationContext)
        }
    }


    private fun checkFirstLogin() {
        if (!readSharedSettingBoolean(applicationContext,
                getString(R.string.sharedpref_first_login), false)
        ) {
            onFirstLogin()
        } else {
            afterFirstLoginDone()
        }
    }

    private fun onFirstLogin() {
        if (!readSharedSettingBoolean(applicationContext,
                getString(R.string.sharedpref_consent),
                false)
        ) {
            Log.d("MainActivity", "Start consent fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, ConsentFragment(), null,
                "consentFragment", true)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(applicationContext,
                getString(R.string.sharedpref_user_info), false)
        ) {
            Log.d("MainActivity", "Start userinfo fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, UserInfoFragment(), null,
                "userInfoFragment", true)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(applicationContext,
                getString(R.string.sharedpref_onboarding), false)
        ) {
            // Set user id for crash reports
            FirebaseCrashlytics.getInstance().setUserId(
                Utils.readSharedSettingString(Graph.appContext,
                    getString(R.string.sharedpref_userid),
                    "")
                    .toString()
            )
            Log.d("MainActivity", "Start onboarding activity")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, OnboardingFragment(), null,
            "onboardingFragment", true)
            bottomNav.isVisible = false
        }
        else {
            saveSharedSettingBoolean(Graph.appContext, getString(R.string.sharedpref_first_login), true)
        }
    }

    private fun afterFirstLoginDone() {
        //if (!homeFragment.isVisible) {
            //loadFragment(this, homeFragment, null, "homeFragment", false)
          //  bottomNav.isVisible = true
        //}
        bottomNav.isVisible = true
        if (!supportFragmentManager.isStateSaved) {
            //Check which fragment is visible
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            if (currentFragment == null) {
                loadFragment(this, homeFragment, null, "homeFragment", true)
            }
        }
        checkPermissions()
        checkDayOfQuestionnaire()
        if (!readSharedSettingBoolean(applicationContext, getString(
                R.string.sharedpref_accessibility), false)) {
            Log.d("afterFirstLoginDone", "check accessibility")
            Utils.showAlertDialog(this@MainActivity, 0)
        } else if (!readSharedSettingBoolean(applicationContext,
                getString(R.string.sharedpref_battery), false)) {
            Log.d("afterFirstLoginDone", "check battery")
            Utils.showAlertDialog(this@MainActivity, 1)
        }
        //checkPermissions()
    }

    fun schedulePushNotifications() {
        val calendar = GregorianCalendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= HOUR_TO_SHOW_PUSH) {
                add(Calendar.DAY_OF_MONTH, 1)
            }

            set(Calendar.HOUR_OF_DAY, HOUR_TO_SHOW_PUSH)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            alarmPendingIntent
        )
    }
}
