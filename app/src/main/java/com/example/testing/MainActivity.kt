package com.example.testing

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

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
    private lateinit var progressBar: ProgressBar
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)
        progressBar = findViewById(R.id.circle_progress_bar)
        showPercentage(0.23)
        //loadFragment(HomeFragment())
        // Initialize the bottom navigation view
        bottomNav = findViewById(R.id.bottomNav)
        val navController = findNavController(R.id.nav_fragment)
        bottomNav.setupWithNavController(navController)
        /**
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.settingsFragment -> {
                    loadFragment(SettingsFragment())
                    true
                }
                R.id.chartFragment -> {
                    loadFragment(ChartFragment())
                    true
                }
                else -> {
                    false
                }
            }
        }*/
        checkAccessibilityPermission()
        binding.FitbitBtn.setOnClickListener {
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
        }
    }

    /** Check accessibility permissions again if not provided when returning to the app **/
    override fun onResume() {
        super.onResume()
        checkAccessibilityPermission()
    }

    /**
     * Show the opposite of error rate to indicate how accurate
     * the user has been in typing words
     */
    fun showPercentage(errorRate: Double) {
        var successRate = (1.0 - errorRate) * 100
        progressBar.progress = successRate.toInt()

    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
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
            view.showSnackbar(
                view, getString(R.string.permission_required),
                Snackbar.LENGTH_INDEFINITE, "OK"
            ) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                /** request permission via start activity for result  */
                startActivity(intent)
            }
            false
        } else {
            view.showSnackbar(
                view, getString(R.string.permission_granted),
                Snackbar.LENGTH_SHORT, null
            ) {}
            true
        }
    }

    fun View.showSnackbar(
        view: View,
        msg: String,
        length: Int,
        actionMessage: CharSequence?,
        action: (View) -> Unit,
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
}
