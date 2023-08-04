package com.example.testing.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.data.KeyboardStats
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class Utils {

    companion object {
        var keyboardList: ArrayList<KeyboardStats> = arrayListOf()
        var errorsList: MutableList<Long> = mutableListOf()
        var totalErrList: MutableList<Long> = mutableListOf()
        var totalSpeedsList: MutableList<MutableList<Double>> = mutableListOf()
        var totalAvgErrors: ArrayList<Long> = arrayListOf()
        var timeWindow: Int = 0
        var totalErr: Double = 0.0
        var totalSpeed: Double = 0.0
        var speedsList: MutableList<Double> = mutableListOf()
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("dd-MM-yyyy")
        val formatterChart = SimpleDateFormat("dd-MM")
        var currentDate: String = getCurrentDateString()
        private val questionnaireCompleteString = "QuestionnaireCompleted"


        fun formatForFitbit(inputDate: String): String {
            val inputFormatter = SimpleDateFormat("dd-MM-yyyy")
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val date = inputFormatter.parse(inputDate) as Date
            val returnDate = outputFormatter.format(date)
            return returnDate
        }


        fun getCurrentDateString(): String {
            val time = Calendar.getInstance().time
            val currentDay = formatter.format(time)
            return currentDay
        }

        fun formatDateStringFromFitbit(inputDate: String): String {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val outputFormatter = SimpleDateFormat("dd-MM-yyyy")
            val date = inputFormatter.parse(inputDate) as Date
            val returnDate = outputFormatter.format(date)
            return returnDate
        }

        /**
         * Get the date previous to the currently SELECTED date
         */
        fun getPreviousDateString(inputDate: String): String {
            val cal = Calendar.getInstance()
            var date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, -1)
            var previousDate = formatter.format(cal.time)
            return previousDate
        }

        fun getDateWeekFromNow(inputDate: String): String {
            val cal = Calendar.getInstance()
            val date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, +7)
            return formatter.format(cal.time)
        }

        /**
         * Get the date after the currently SELECTED date
         */
        fun getNextDateString(inputDate: String): String {
            val cal = Calendar.getInstance()
            var date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, +1)
            var nextDate = formatter.format(cal.time)
            return nextDate

        }

        fun formatDateForChart(inputDate: String): String {
            val inputFormatter = SimpleDateFormat("dd-MM-yyyy")
            val outputFormatter = SimpleDateFormat("dd-MM")
            val date = inputFormatter.parse(inputDate) as Date
            return outputFormatter.format(date)
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

        /**
         * Return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
         */
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val permission = ContextCompat.checkSelfPermission(Graph.appContext, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            if (permission == PackageManager.PERMISSION_DENIED) {
                val pwrm =
                    context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                val name = context.applicationContext.packageName
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    saveSharedSettingBoolean("battery_opt_off",
                        pwrm.isIgnoringBatteryOptimizations(name))
                    return pwrm.isIgnoringBatteryOptimizations(name)
                }

                saveSharedSettingBoolean("battery_opt_off", false)
                return false
            } else return true
        }

        /**
         * Check if optimization is enabled
         */
        fun checkBattery(context: Context) {
            if (!isIgnoringBatteryOptimizations(context!!.applicationContext) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val name = R.string.app_name
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                Toast.makeText(context, R.string.battery_optimization_prompt,
                    Toast.LENGTH_SHORT).show()
                context.startActivity(intent)
            }
        }

        fun readSharedSettingBoolean(
            settingName: String?, defaultValue: Boolean
        ): Boolean {
            val s = getSharedPrefs()
            return s.getBoolean(settingName, defaultValue)
        }

        fun readSharedSettingString(
            settingName: String?, defaultValue: String
        ): String? {
            val s = getSharedPrefs()
            return s.getString(settingName, defaultValue)
        }

        fun readSharedSettingInt(
            settingName: String?, defaultValue: Int
        ): Int? {
            val s = getSharedPrefs()
            return s.getInt(settingName, defaultValue)
        }

        fun getSharedPrefs(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
        }


        fun saveSharedSetting(
            settingName: String?, settingValue: String?
        ) {
            val s = getSharedPrefs()
            val editor = s.edit()
            editor.putString(settingName, settingValue)
            editor.apply()
        }

        fun saveSharedSettingBoolean(
            settingName: String?, settingValue: Boolean
        ) {
            val s = getSharedPrefs()
            val editor = s.edit()
            editor.putBoolean(settingName, settingValue)
            editor.apply()
        }

        fun saveSharedSettingInt(
            settingName: String?, settingValue: Int
        ) {
            val s = getSharedPrefs()
            val editor = s.edit()
            editor.putInt(settingName, settingValue)
            editor.apply()
        }

        fun checkPermissions(context: Context = Graph.appContext): Boolean {
            isIgnoringBatteryOptimizations(context)
            val batteryOptOff = readSharedSettingBoolean( "battery_opt_off", false)
            val consent = readSharedSettingBoolean("consent_given", true)
            val userInfoSaved = readSharedSettingBoolean("user_info_saved", true)
            val accessibilityPermission = checkAccessibilityPermission(context, false)
            if (consent && batteryOptOff && userInfoSaved && accessibilityPermission) {
                val sharedPref = getSharedPrefs()
                val editor = sharedPref.edit()
                editor.putBoolean("permissions_granted", true).apply()
                return true
            }
            return false
        }

        fun checkAccessibilityPermission(
            context: Context = Graph.appContext,
            openSettings: Boolean
        ): Boolean {
            var accessEnabled = 0
            try {
                accessEnabled =
                    Settings.Secure.getInt(context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED)
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
            when (accessEnabled) {
                0 -> return if (openSettings) {
                    Toast.makeText(context, R.string.accessibility_permission_required,
                        Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    // request permission via start activity for result
                    context.startActivity(intent)
                    false
                } else {
                    saveSharedSettingBoolean("accessibility_permission", false)
                    false
                }
                else -> return if (openSettings) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    true
                } else {
                    saveSharedSettingBoolean("accessibility_permission", true)
                    true
                }
            }
        }

    }
}
