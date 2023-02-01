package com.example.testing

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.example.testing.ui.onboarding.ConsentActivity
import com.example.testing.ui.onboarding.OnboardingActivity
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
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
 * Q2: How often did you have to correct your typing? (Scale 1-7)
 * Q3: At what time of day were you most active with typing? (some kind of selector)
 */
class MainActivity : AppCompatActivity() {

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
        val sharedPrefs = getSharedPreferences("USER_INFO", MODE_PRIVATE)
        sharedPrefs.apply {
            // Check if we need to display our OnboardingSupportFragment
            if (!getBoolean("onboarding_complete", false)) {
                // The user hasn't seen the OnboardingSupportFragment yet, so show it
                startActivity(Intent(this@MainActivity, ConsentActivity::class.java))
            }
        }
        loadFragment(this, homeFragment, null, "homeFragment", true)
        bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.homeFragment
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment ->
                    loadFragment(this, homeFragment, null, "homeFragment", true)
                R.id.settingsFragment ->
                    loadFragment(this, settingsFragment, null, "settingsFragment", true)
                R.id.chartFragment ->
                    loadFragment(this, chartFragment, null, "chartFragment", true)
                }
            true
        }

        checkAccessibilityPermission()
    }

    /**
     * Check accessibility permissions & battery optimization
     *  again if not provided when returning to the app
     **/
    override fun onResume() {
        super.onResume()
        checkAccessibilityPermission()
    }

    /** Check for accessibility permissions **/
    private fun checkAccessibilityPermission(): Boolean {
        var accessEnabled = 0
        try {
            accessEnabled =
                Settings.Secure.getInt(this.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        return if (accessEnabled == 0) {
            // if access not granted, construct intent to request permission
            // don't use view to show snackbar if it should be above bottomNavigationView
            binding.container.showSnackbar(
                view, getString(R.string.permission_required),
                Snackbar.LENGTH_INDEFINITE, "OK"
            ) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // request permission via start activity for result
                startActivity(intent)
            }
            false
        } else {
            binding.container.showSnackbar(
                view, getString(R.string.permission_granted),
                Snackbar.LENGTH_SHORT, null
            ) {}
            true
        }
    }
}
