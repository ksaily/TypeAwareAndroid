package com.example.testing.utils

import android.util.Log
import com.example.testing.MyAccessibilityService
import java.util.concurrent.TimeUnit

class KeyboardHelper {

    companion object {
        var wordCount: Int = 0
        var typingTimes: ArrayList<Double> = arrayListOf()
        var currentPackage: String = ""
        var timeElapsed: Double = 0.0
        var deletedChars: Int = 0
        var timeStampBeginning: Long = 0
        var errorRate: Double = 0.0
        var beforeString: String = ""
        var timeSlots: Int = 0
        var newPackage: String = ""

        fun countTimeSlot(): Int {
            var currentTime = System.currentTimeMillis()
            //Change to hours, multiply by six because there are six time slots in one hour
            var getHours = TimeUnit.MILLISECONDS.toHours(currentTime) * 6
            var getMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime).toInt() / 10
            //Timeslots start from 0
            timeSlots = getHours.toInt() + getMinutes
            Log.d("JobScheduler", "Timeslot is: $timeSlots")

            return timeSlots
        }

        fun countWords(): Int {
            val trimmedStr = beforeString.trim().replace("[^a-zA-Z]+".toRegex(), " ")
            return if (trimmedStr.isEmpty()) {
                0
            } else {
                var newStr = trimmedStr.split("\\s+".toRegex())
                Log.d("KeyboardEvents", "Trimmed words is: $newStr")
                Log.d("KeyboardEvents", "Amount of words written: ${newStr.size}")
                wordCount = newStr.size
                newStr.size
            }
        }

        fun sameSession(session: String, timeElapsed: Double): Boolean {
            return ((session == currentPackage) && (timeElapsed < 10.0))
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
                    var newStr = beforeString.substring(0, beforeString.length - 1)
                    Log.d("KeyboardEvents", "String after deleting a char: $newStr")
                    beforeString = newStr
                } else {
                    var newChar = text.last()
                    beforeString += newChar
                    Log.d("KeyboardEvents", "New char is: $newChar")
                    Log.d("KeyboardEvents", "Current string is: $beforeString")
                }
            } catch (e: NoSuchElementException) {
            }
        }

        fun countErrorRate(): Double {
            return  deletedChars / wordCount.toDouble()
        }
    }
}