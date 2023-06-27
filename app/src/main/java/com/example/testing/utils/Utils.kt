package com.example.testing.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.Global.getString
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.testing.Graph
import com.example.testing.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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


        /**
         * Count average for one instance in firebase database,
         * which is a list of typing speeds and
         * return the average speed for that instance
         */
        fun countAvgSpeed(speed: MutableList<Double>): Double {
            var total = 0.0
            for (i in speed) {
                total += i
            }
            return total / speed.size
        }

        /**
         * Count average for one day
         * from a list of errors and
         * return the average errors for that day
         */
        fun countAvgErrors(errors: MutableList<Long>): Double {
            var total = 0.0
            for (i in errors) {
                total += i
            }
            return total / errors.size
        }

        fun formatForFitbit(inputDate: String): String {
            val inputFormatter = SimpleDateFormat("dd-MM-yyyy")
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val date = inputFormatter.parse(inputDate) as Date
            val returnDate = outputFormatter.format(date)
            Log.d("DateFormat", "Date formatted from: $date to $returnDate")
            return returnDate
        }


        fun getCurrentDateString(): String {
            val time = Calendar.getInstance().time
            val currentDay = formatter.format(time)
            Log.d("CurrentDate", "Current date is $currentDay")
            return currentDay
        }

        fun formatDateStringFromFitbit(inputDate: String): String {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val outputFormatter = SimpleDateFormat("dd-MM-yyyy")
            val date = inputFormatter.parse(inputDate) as Date
            val returnDate = outputFormatter.format(date)
            Log.d("DateFormat", "Date formatted from $inputDate to: $returnDate")
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
            Log.d("Dates", "Selected date: $previousDate")
            Log.d("Dates", "Previous date: $date")
            return previousDate
        }

        fun getDateWeekFromNow(inputDate: String): String {
            val cal = Calendar.getInstance()
            val date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, +7)
            return formatter.format(cal.time)
        }

        fun dateHasPassed(inputDate: String): Boolean {
            val date = formatter.parse(inputDate) as Date
            val thisDay = formatter.parse(currentDate) as Date
            //Check whether date is today or has passed
            return date.before(thisDay) || date == thisDay
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
            Log.d("Dates", "Current date: $nextDate")
            Log.d("Dates", "Previous date: $date")
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
            val pwrm =
                context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                saveSharedSettingBoolean(Graph.appContext, "battery_opt_off",
                    pwrm.isIgnoringBatteryOptimizations(name))
                return pwrm.isIgnoringBatteryOptimizations(name)
            }
            saveSharedSettingBoolean(Graph.appContext, "battery_opt_off", false)
            return false
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
            ctx: Context = Graph.appContext,
            settingName: String?, defaultValue: Boolean
        ): Boolean {
            val s = PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
            val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            return s.getBoolean(settingName, defaultValue)
        }

        fun readSharedSettingString(
            ctx: Context = Graph.appContext,
            settingName: String?, defaultValue: String
        ): String? {
            val s = PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
            val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            return s.getString(settingName, defaultValue)
        }

        fun getSharedPrefs(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
        }


        fun saveSharedSetting(
            ctx: Context = Graph.appContext,
            settingName: String?, settingValue: String?
        ) {
            val s = PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
            val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            val editor = s.edit()
            editor.putString(settingName, settingValue)
            editor.apply()
        }

        fun saveSharedSettingBoolean(
            ctx: Context = Graph.appContext,
            settingName: String?, settingValue: Boolean
        ) {
            val s = PreferenceManager.getDefaultSharedPreferences(Graph.appContext)
            //val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            val editor = s.edit()
            editor.putBoolean(settingName, settingValue)
            editor.apply()
        }

        fun checkPermissions(context: Context = Graph.appContext): Boolean {
            isIgnoringBatteryOptimizations(context)
            val batteryOptOff = readSharedSettingBoolean(context, "battery_opt_off", false)
            val consent = readSharedSettingBoolean(context, "consent_given", true)
            val userInfoSaved = readSharedSettingBoolean(context, "user_info_saved", true)
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
            Log.d("AccessibilitySettings", accessEnabled.toString())
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
                    Log.d("AccessibilitySettings", "value false")
                    saveSharedSettingBoolean(
                        context, "accessibility_permission", false)
                    false
                }
                else -> return if (openSettings) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    true
                } else {
                    Log.d("AccessibilitySettings", "value true")

                    saveSharedSettingBoolean(
                        context, "accessibility_permission", true)
                    true
                }
            }
        }

        fun checkQuestionnaireWeek(startDay: String, weekNumber: Int) {
            var nextDay: String = ""
            if (weekNumber == 1) {
                nextDay = readSharedSettingString(Graph.appContext, "questionnaire_first_day", "").toString()
                //val nextDay = getNextDateString(startDay)
            }
            else if (weekNumber == 2) {
                nextDay = readSharedSettingString(Graph.appContext, "second_week_start_date", "").toString()
            }
            //val nextDay = getNextDateString(startDay)
            val participantId = readSharedSettingString(Graph.appContext, "userId", "").toString()
            val myRef = Firebase.database.getReference("Data")
            var isQuestionnaireAnswered = false
            var amountOfAnswers = 0
            val dayAfter = getNextDateString(currentDate)
            while (nextDay != dayAfter) {
                myRef.child(participantId).child(nextDay).child("questionnaire")
                    .child(questionnaireCompleteString).get().addOnSuccessListener { snapshot ->
                        Log.d("Firebase", "Questionnaire listener")
                        isQuestionnaireAnswered = (snapshot.exists() &&
                                snapshot.value as Boolean)
                    }.addOnFailureListener {
                        // Error occurred while checking if questionnaire is answered, show home screen
                        Log.d("checkQuestionnaireWeek", "Failure on setting listener")
                    }
                if (isQuestionnaireAnswered) {
                    amountOfAnswers ++
                }
                nextDay = getNextDateString(nextDay)
            }
            if (amountOfAnswers == 7) {
                if (weekNumber == 1) {
                    saveSharedSettingBoolean(Graph.appContext,
                        "first_week_done", true)
                    saveSharedSetting(Graph.appContext, "second_week_start_date", currentDate)
                    saveSharedSetting(Graph.appContext, "second_week_end_date", getDateWeekFromNow(
                        currentDate))
                } else if (weekNumber == 2) {
                    saveSharedSettingBoolean(Graph.appContext,
                        "second_week_done", true)
                }
            }
        }

        /**
         * @param action 0 for accessibility settings, 1 for battery optimization settings
         */
        fun showAlertDialog(context: Context, action: Int) {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)
            val alert: AlertDialog = alertDialog.create()
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
                        checkAccessibilityPermission(Graph.appContext, true)
                    }

                    1 -> {
                        checkBattery(Graph.appContext)
                    }
                }
                alertDialog.setNegativeButton(
                    "Cancel"
                ) { _, _ ->
                    if (alert.isShowing) {
                        alert.dismiss()
                    }
                }
                alert.setCanceledOnTouchOutside(true)
                alert.show()
            }
        }
    }
}