package com.example.testing.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.testing.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Utils {

    companion object {
        var keyboardList: ArrayList<KeyboardStats> = arrayListOf()
        var errorsList: MutableList<Long> = mutableListOf()
        var totalErrList: MutableList<Long> = mutableListOf()
        var totalSpeedsList: MutableList<MutableList<Double>> = mutableListOf()
        var totalAvgErrors: ArrayList<Long> = arrayListOf()
        var timeWindow: String = ""
        var totalErr: Double = 0.0
        var totalSpeed: Double = 0.0
        var speedsList: MutableList<Double> = mutableListOf()
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        var currentDate: String = getCurrentDateString()

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

        fun getCurrentDateString(): String {
            var time = Calendar.getInstance().time
            return formatter.format(time)
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
            Log.d("Dates", "Selected date: $inputDate")
            Log.d("Dates", "Previous date: $date")
            return previousDate
        }

        /**
         * Get the date after the currently SELECTED date
         */
        fun getNextDateString(inputDate: String): String {
            val cal = Calendar.getInstance()
            var date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, +1)
            var inputDate = formatter.format(cal.time)
            Log.d("Dates", "Selected date: $inputDate")
            Log.d("Dates", "Previous date: $date")
            return inputDate

        }

        fun getFromFirebase(date: String) {
            val rootRef = FirebaseDatabase.getInstance().reference
            val ref = rootRef.child(date)
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val children = snapshot.children
                        if (children != null) {
                            children.forEach { dataSnapshot ->
                                var child = dataSnapshot.children
                                child.forEach {
                                    var speeds = it.child("typingSpeed").value
                                    var avgForOne = countAvgSpeed(speeds as MutableList<Double>)
                                    errorsList.add(it.child("errorAmount").value as Long)
                                    //Add the average for one instance to a new list
                                    speedsList.add(avgForOne)
                                }
                                totalErrList = (totalErrList + errorsList).toMutableList()
                                Log.d("Firebase", child.toString())
                                totalSpeedsList.add(speedsList.toMutableList())
                                timeWindow = dataSnapshot.key.toString()
                                //avgSpeed = countAvgSpeed(totalAvgSpeed)
                                //var data = KeyboardStats(date, dataSnapshot.key, avgErrors, avgSpeed)
                                //println(data)
                            }
                            totalErr = countAvgErrors(totalErrList)
                            var total: MutableList<Double> = mutableListOf()
                            for (i in totalSpeedsList) {
                                total.add(countAvgSpeed(i))
                            }
                            totalSpeed = countAvgSpeed(total)
                            var data = KeyboardStats(
                                currentDate,
                                timeWindow,
                                totalErr,
                                totalSpeed)
                            Log.d("Firebase", "Data fetched from firebase")
                            println(data)
                            keyboardList.add(data)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Firebase", error.message)
                }
            }
            if (date != currentDate) {
                rootRef.addListenerForSingleValueEvent(valueEventListener)
            } else {
                ref.addValueEventListener(valueEventListener)
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

        /**
         * Return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
         */
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val name = context.applicationContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return pwrm.isIgnoringBatteryOptimizations(name)
            }
            return true
        }

        /**
         * Check if optimization is enabled
         */
        fun checkBattery(context: Context) {
            if (!isIgnoringBatteryOptimizations(context!!.applicationContext) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val name = R.string.app_name
                Toast.makeText(context!!.applicationContext, "Battery optimization -> All apps -> $name -> Don't optimize", Toast.LENGTH_LONG).show()

                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.applicationContext.startActivity(intent)
            }
        }

        fun readSharedSetting(ctx: Context, settingName: String?, defaultValue: String?): String? {
            val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            return sharedPref.getString(settingName, defaultValue)
        }

        fun saveSharedSetting(ctx: Context, settingName: String?, settingValue: String?) {
            val sharedPref = ctx.getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString(settingName, settingValue)
            editor.apply()
        }
    }
}