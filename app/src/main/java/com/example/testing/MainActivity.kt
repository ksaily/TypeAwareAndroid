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
import com.example.testing.databinding.ActivityMainBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)
        loadFragment(HomeFragment())
        //chart = binding.barChart //this is our barchart
        //binding.barChartTitleTV.text = "$year Sales"
        /**
        var values1: ArrayList<BarEntry> = ArrayList()
        var values2: ArrayList<BarEntry> = ArrayList()
        statValues.clear()
        //
            for (i in 0 until MAX_X_VALUE) {
                values1.add(
                        BarEntry(
                            i.toFloat(),
                            (Math.random() * 80).toFloat()
                            )
                        )
            }

        for (i in 0 until MAX_X_VALUE) {
            values2.add(
                BarEntry(
                    i.toFloat(),
                    (Math.random() * 80).toFloat()
                )
            )
        }
        //
        ////After preparing our data set, we need to display the data in our bar chart
        val dataSet1: BarDataSet = BarDataSet(values1, "Test")
        val dataSet2: BarDataSet = BarDataSet(values2, "Test")
        val data: BarData = BarData()
            data.addDataSet(dataSet1)
            data.addDataSet(dataSet2)
        configureBarChart()
        prepareChartData(data)*/

        bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.homeFragment
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment ->
                    loadFragment(HomeFragment())
                R.id.settingsFragment ->
                    loadFragment(SettingsFragment())
                R.id.chartFragment ->
                    loadFragment(ChartFragment())
                }
            true
        }

        /**val navController = findNavController(R.id.nav_host_fragment)
        val appBarConf = AppBarConfiguration(setOf(

            R.id.homeFragment, R.id.navigation_chat

        ))

        setupActionBarWithNavController(navController, appBarConf)
        navView.setupWithNavController(navController)*/
        checkAccessibilityPermission()
    }
/**
    private fun prepareChartData(data: BarData) {
        chart!!.data = data
        chart!!.barData.barWidth = BAR_WIDTH
        val groupSpace = 1f - (BAR_SPACE + BAR_WIDTH)
        chart!!.groupBars(0f, groupSpace, BAR_SPACE)
        chart!!.invalidate()
    }

    private fun configureBarChart() {
        chart!!.setPinchZoom(false)
        chart!!.setDrawBarShadow(false)
        chart!!.setDrawGridBackground(false)

        chart!!.description.isEnabled = false
        val xAxis = chart!!.xAxis
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawGridLines(false)
        val leftAxis = chart!!.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.spaceTop = 35f
        leftAxis.axisMinimum = 0f
        chart!!.axisRight.isEnabled = false
        chart!!.xAxis.axisMinimum = 1f
        chart!!.xAxis.axisMaximum = MAX_X_VALUE.toFloat()
    }
*/


    /** Check accessibility permissions again if not provided when returning to the app **/
    override fun onResume() {
        super.onResume()
        checkAccessibilityPermission()
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
