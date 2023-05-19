package com.example.testing

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.menu.DateFragment
import com.example.testing.ui.onboarding.*
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.ui.viewmodel.PrefsViewModel
import com.example.testing.utils.FragmentUtils
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.FragmentUtils.Companion.removeFragmentByTag
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.checkBattery
import com.example.testing.utils.Utils.Companion.getSharedPrefs
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import com.example.testing.utils.Utils.Companion.saveSharedSettingBoolean
import com.example.testing.utils.Utils.Companion.showSnackbar
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

/**
 * First week, show only survey for the user: How do you think you did this week
 * Second week, survey first and then reveal them the data
 * Q1: How much time did you spend typing during date x?
 * (Compare the participant's performance to others, how they feel about it)
 * Q2: How often did you have to correct your typing? (Scale 1-7)
 * Q3: At what time of day were you most active with typing? (some kind of selector)
 */
class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    private lateinit var view: View
    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var handler: Handler

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
    val dateViewModel: DateViewModel by viewModels()
    val firebaseViewModel: FirebaseViewModel by viewModels()
    private val calendar: Calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)
    private val homeFragment = HomeFragment()
    private val chartFragment = ChartFragment()
    private val settingsFragment = SettingsFragment()
    val database = Firebase.database("https://health-app-9c151-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)
        val sharedPrefs = Utils.getSharedPrefs()
        val transaction = supportFragmentManager.beginTransaction()

        //Utils.checkBattery(applicationContext)

        if (!readSharedSettingBoolean(applicationContext,
                "consent_given",
                false)) {
            loadFragment(this, ConsentFragment(), null,
                "consentFragment", false)
        }

        loadFragment(this, homeFragment, null, "homeFragment", true)

        bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.homeFragment
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment -> {
                    Log.d("BottomNav", "homefragment selected")
                    loadFragment(this, homeFragment, null, "homeFragment", true)
                }
                R.id.settingsFragment -> {
                    Log.d("BottomNav", "settingfragment selected")
                    loadFragment(this, settingsFragment, null, "settingsFragment", true)
                }
                R.id.chartFragment -> {
                    Log.d("BottomNav", "chartfragment selected")
                    loadFragment(this, chartFragment, null, "chartFragment", true)
                }
            }
            true
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
        if (!readSharedSettingBoolean(applicationContext,
                "first_login_done", false)
        ) {
            onFirstLogin()
        }
        //onFirstLogin()

        Utils.checkPermissions(applicationContext)
    }

    /**
     * Check accessibility permissions & battery optimization
     *  again if not provided when returning to the app
     **/
    override fun onResume() {
        super.onResume()
        //Register on shared preference change listener in onCreate and check for permissions?
        Utils.checkPermissions(applicationContext)
        /**if (!readSharedSettingBoolean(applicationContext,
                "first_login_done", false)
        ) {
            onFirstLogin()
        }**/
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPrefs().unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if (key == "accessibility_permission" && !readSharedSettingBoolean(Graph.appContext,
                "accessibility_permission", false)
        ) {
            Log.d("CheckPref", "Accessibility")
            Utils.checkAccessibilityPermission(applicationContext, true)
        }
        if (key == "battery_opt_off" && !readSharedSettingBoolean(Graph.appContext,
                "accessibility_permission", false)
        ) {
            Log.d("CheckPref", "Battery")
            checkBattery(applicationContext)
        }

    }

    private fun onFirstLogin() {
        if (!readSharedSettingBoolean(applicationContext,
                "consent_given",
                false)
        ) {
            Log.d("MainActivity", "Start consent fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, ConsentFragment(), null,
                "consentFragment", false)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(applicationContext,
            "user_info_saved", false)
        ) {
            Log.d("MainActivity", "Start userinfo fragment")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, UserInfoFragment(), null,
                "userInfoFragment", false)
            bottomNav.isVisible = false
        }

        else if (!readSharedSettingBoolean(applicationContext,
            "onboarding_complete", false)
        ) {
            Log.d("MainActivity", "Start onboarding activity")
            // The user hasn't seen the onboarding & consent screens yet, so show it
            loadFragment(this, OnboardingFragment(), null,
            "onboardingFragment", true)
            bottomNav.isVisible = false
        }
        else {
            saveSharedSettingBoolean(Graph.appContext, "first_login_done", true)
            loadFragment(this, homeFragment, null, "homeFragment", true)
            //val consentFragment = supportFragmentManager.findFragmentByTag("consentFragment")
            bottomNav.isVisible = true
        }
    }
}
