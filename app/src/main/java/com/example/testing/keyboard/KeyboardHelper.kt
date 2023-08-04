package com.example.testing.keyboard

import android.util.Log
import com.example.testing.data.KeyboardEvents
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class KeyboardHelper {

    companion object {
        var wordCount: Int = 0
        var typingTimes: ArrayList<Double> = arrayListOf()
        var thisPackage: String = ""
        var timeElapsed: Double = 0.0
        var deletedChars: Int = 0
        var deletedCharsAfterSessionChange: Int = 0
        var timeStampBeginning: Long = 0
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
            //Timeslots start from 1
            val currentTimeSlot = getHours + getMinutes

            return currentTimeSlot
        }

        /**
        fun countWords(): Int {
            val trimmedStr = beforeString.replace("[^a-zA-Z]+".toRegex(), " ").trim()
            return if (trimmedStr.isEmpty()) {
                0
            } else {
                val newStr = trimmedStr.split("\\s+".toRegex())
                Log.d("KeyboardEvents", "Amount of words written: ${newStr.size}")
                //wordCount = newStr.size
                //newStr.size
            }
        }**/

        fun checkSameSession(session: String, timeElapsedd: Double): Boolean {
            return (session == thisPackage) && (timeElapsedd < 10.0)
        }

        fun addToString(text: String, beforeText: String, sameSession: Boolean, removedChars: Int) {
            try {
                if (removedChars != 0) {
                    if (sameSession) {
                        deletedChars += removedChars
                    }
                } else if (text.isNotEmpty()) {
                    var newChar = text.last()
                    newChar = if (newChar.isLetterOrDigit()) {
                        'a'
                    } else {
                        ' ' // Remove symbols
                    }
                    val isWordStart = checkWordStart(newChar, beforeText)
                    val isWordEnd = checkWordEnd(newChar, beforeText)

                    if (sameSession) {
                        if (isWordEnd && beforeString.isNotEmpty()) {
                            wordCount++ // Count words even if characters would be deleted after
                            endTime = System.nanoTime()
                            // To seconds
                            timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                            typingTimes.add(timeElapsed)

                        }
                        if (isWordStart) {
                            startTime = System.nanoTime()
                        }
                        // Record endtime in case of session change
                        endTime = System.nanoTime()

                        beforeString += newChar
                        //Log.d("KeyboardEvents", "New char is: $newChar")
                    } else {
                        val t = if (!beforeString.isNullOrEmpty()) {
                             beforeString.last()
                        } else {
                            ' '
                        }

                        if (t.isLetterOrDigit()) {
                            // Only save word if the word is not already saved
                            // occurs when previous session ends in a space
                            wordCount++
                            timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                            if (timeElapsed > 0) {
                                typingTimes.add(timeElapsed)
                            }
                        }
                        //Different sessions between newchar and beforechar
                        if (newChar.isLetterOrDigit()) {
                            startTime = System.nanoTime()
                        }
                        newString += newChar
                        //Log.d("KeyboardEvents", "New char is: $newChar")
                    }
                } else {
                    beforeString += ' '
                    startTime = System.nanoTime()
                }
            }
            catch (e: Error) {
                Log.d("Error", "$e")
            }
        }

        private fun checkWordStart(currentChar: Char, beforeTxt: String): Boolean {
            // Check if the character is the first character of a word (after a space or empty field)
            val isSpaceBefore: Boolean = if (beforeTxt.isNotEmpty()) {
                !beforeTxt.last().isLetterOrDigit()
            } else {
                true
            }
            return isSpaceBefore && currentChar.isLetterOrDigit()
        }

        private fun checkWordEnd(currentChar: Char, beforeTxt: String): Boolean {
            return if (beforeTxt.isEmpty()) {
                currentChar.isLetterOrDigit()
            } else {
                beforeTxt.last().isLetterOrDigit() && !currentChar.isLetterOrDigit()
            }
        }

        fun countErrorRate(): Double {
            return  deletedChars.toDouble() / (deletedChars + (beforeString.length))
        }

        fun dateFormatter(date: Date): String {
            val formatter = SimpleDateFormat("dd-MM-yyyy")
            return formatter.format(date)
        }
    }
}