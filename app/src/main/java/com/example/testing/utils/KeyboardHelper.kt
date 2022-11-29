package com.example.testing.utils

import android.util.Log
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

class KeyboardHelper {

    companion object {
        private var wordCount: Int = 0
        var typingTimes: ArrayList<Double> = arrayListOf()
        var thisPackage: String = ""
        var timeElapsed: Double = 0.0
        var deletedChars: Int = 0
        var timeStampBeginning: Long = 0
        var errorRate: Double = 0.0
        var beforeString: String = ""
        var currentTimeSlot: Int = 0
        var previousTimeSlot: Int = 0
        var newPackage: String = ""
        var dataList: MutableList<KeyboardEvents> = mutableListOf()

        /** Count the current timeslot (10 minute windows) **/
        fun countTimeSlot(): Int {
            val currentTime = Calendar.getInstance()
            //Change to hours, multiply by six because there are six time slots in one hour
            val getHours = currentTime.get(Calendar.HOUR_OF_DAY) * 6 + 1
            val getMinutes = currentTime.get(Calendar.MINUTE) / 10
            //Timeslots start from 0
            Log.d("KeyboardEvents","Current hours slot: $getHours")
            Log.d("KeyboardEvents","Current minutes slot: $getMinutes")
            val currentTimeSlot = getHours + getMinutes
            Log.d("KeyboardEvents", "Timeslot is: $currentTimeSlot")

            return currentTimeSlot
        }

        fun countWords(): Int {
            val trimmedStr = beforeString.trim().replace("[^a-zA-Z]+".toRegex(), " ")
            return if (trimmedStr.isEmpty()) {
                0
            } else {
                val newStr = trimmedStr.split("\\s+".toRegex())
                Log.d("KeyboardEvents", "Trimmed words is: $newStr")
                Log.d("KeyboardEvents", "Amount of words written: ${newStr.size}")
                wordCount = newStr.size
                newStr.size
            }
        }

        fun sameSession(session: String, timeElapsed: Double): Boolean {
            return ((session == thisPackage) && (timeElapsed < 10.0))
        }

        fun checkDeletedChars(currentText: String, beforeText: String): Boolean {
            return if (currentText.length < beforeText.length && beforeString.isNotEmpty()) {
                deletedChars ++
                true
            } else { false }
        }

        fun addToString(text: String, beforeText: String) {
            try {
                if (checkDeletedChars(text, beforeText)) {
                    Log.d("KeyboardEvents", "String before deleting a char: $beforeString")
                    val newStr = beforeString.substring(0, beforeString.length - 1)
                    Log.d("KeyboardEvents", "String after deleting a char: $newStr")
                    beforeString = newStr
                } else {
                    val newChar = text.last()
                    beforeString += newChar
                    Log.d("KeyboardEvents", "New char is: $newChar")
                    Log.d("KeyboardEvents", "Current string is: $beforeString")
                }
            } catch (_: NoSuchElementException) {
            }
        }

        fun countErrorRate(): Double {
            return  deletedChars.toDouble() / (beforeString.length - 1)
        }
    }
}