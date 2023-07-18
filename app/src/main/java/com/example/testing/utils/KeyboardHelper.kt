package com.example.testing.utils

import android.util.Log
import com.example.testing.data.KeyboardEvents
import java.text.SimpleDateFormat
import java.util.*
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
        var newString: String = ""
        var startTime: Long = 0L
        var endTime: Long = 0L

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
            val trimmedStr = beforeString.replace("[^a-zA-Z]+".toRegex(), " ").trim()
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

        fun checkSameSession(session: String, timeElapsedd: Double): Boolean {
            return (session == thisPackage) && (timeElapsedd < 10.0)
        }

        fun checkDeletedChars(currentText: String, beforeText: String): Boolean {
            return currentText.length < beforeText.length && beforeString.isNotEmpty()
        }

        fun addToString(text: String, beforeText: String, sameSession: Boolean) {
            try {

                if (checkDeletedChars(text, beforeText)) {
                    Log.d("KeyboardEvents", "String before deleting a char: $beforeString")
                    val newStr = beforeString.substring(0, beforeString.length - 1)
                    if (sameSession) {
                        Log.d("KeyboardEvents", "String after deleting a char: $newStr")
                        deletedChars++
                        beforeString = newStr
                    } else { //In case new session is started by deleting a char
                        Log.d("KeyboardEvents", "String after deleting a char: $newStr")
                        deletedChars = 1
                    }
                } else if (text.isNotEmpty()) {
                    var newChar = text.last()
                    val isWordStart = checkWordStart(newChar, beforeText)
                    val isWordEnd = checkWordEnd(newChar, beforeText)
                    if (newChar.isLetterOrDigit()) {
                        newChar = 'a'
                    }
                    if (sameSession) {
                        if (isWordStart) {
                            //Log.d("Firstletter", "ofWord")
                            startTime = System.nanoTime()
                        } else if (isWordEnd) {
                            endTime = System.nanoTime()
                            //Log.d("Lastletter", "ofWord")
                            // To seconds
                            timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                            typingTimes.add(timeElapsed)
                        }
                        // Record endtime in case of session change
                        endTime = System.nanoTime()

                        beforeString += newChar
                        //Log.d("KeyboardEvents", "New char is: $newChar")
                        Log.d("KeyboardEvents", "Current string is: $beforeString")
                    } else {
                        //Different sessions between newchar and beforechar
                        timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                        if (timeElapsed > 0) {
                            typingTimes.add(timeElapsed)
                        } else {
                            typingTimes.add(0.0)
                        }
                        if (isWordStart) {
                            startTime = System.nanoTime()
                        }
                        newString += newChar
                        //Log.d("KeyboardEvents", "New char is: $newChar")
                        Log.d("KeyboardEvents", "Current string is: $newString")
                    }
                }
            }
            catch (e: Error) {
                Log.d("Error", "$e")
            }
        }

        private fun checkWordStart(currentChar: Char, beforeTxt: String): Boolean {
            // Check if the character is the first character of a word (after a space)
            val isSpaceBefore: Boolean = if (beforeTxt.isNotEmpty()) {
                !beforeTxt.last().isLetterOrDigit()
            } else {
                true //Before text is empty
            }
            return isSpaceBefore && currentChar.isLetterOrDigit()
        }

        private fun checkWordEnd(currentChar: Char, beforeTxt: String): Boolean {
            // Check if the character is the first character of a word (after a space)
            val isLetterBefore: Boolean = if (beforeTxt.isNotEmpty()) {
                beforeTxt.last().isLetterOrDigit()
            } else {
                false
            }
            return isLetterBefore && !currentChar.isLetterOrDigit()
        }

        fun countErrorRate(): Double {
            return  deletedChars.toDouble() / (deletedChars + (beforeString.length - 1))
        }

        fun dateFormatter(date: Date): String {
            val formatter = SimpleDateFormat("dd-MM-yyyy")
            return formatter.format(date)
        }
    }
}