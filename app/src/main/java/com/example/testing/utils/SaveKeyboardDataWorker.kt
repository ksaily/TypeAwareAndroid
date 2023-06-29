package com.example.testing.utils

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.work.*
import com.example.testing.Graph
import java.util.*

class SaveKeyboardDataWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var packageName = ""
    private var text = ""
    private var beforeText = ""


    override fun doWork(): Result {
        return try {
            if (startTime == 0L) {
                packageName = inputData.getString("packageName").toString()
                text = inputData.getString("text").toString()
                beforeText = inputData.getString("beforeText").toString()
                //First character in a session, don't add to typing times
                KeyboardHelper.thisPackage = packageName
                KeyboardHelper.timeElapsed = 0.0
                Log.d("KeyboardEvents", "This is the first char of this session")
            } else {
                endTime = System.nanoTime()
                // Time elapsed in seconds:
                KeyboardHelper.timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                //typingTimes.add(timeElapsed)
            }
            startTime = System.nanoTime()
            KeyboardHelper.timeStampBeginning = System.currentTimeMillis()
            //Do checksession with a background worker so it does
            //not block the start of new events
            checkSession(packageName, text, beforeText)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }


    /** Check if session remains the same.
     * If yes, add written character to string and typing time to an arraylist. **/
    private fun checkSession(packageName: String, text: String, beforeText: String) {

        if (KeyboardHelper.checkSameSession(packageName.toString(), KeyboardHelper.timeElapsed)) {
            //Same session as before
            if (KeyboardHelper.timeElapsed != 0.0) {
                KeyboardHelper.typingTimes.add(KeyboardHelper.timeElapsed)
            }
            KeyboardHelper.addToString(text.toString().removeSurrounding("[", "]"),
                beforeText.toString(), true)
        } else { // Session has changed
            KeyboardHelper.newPackage = packageName.toString()
            //startTime = nanoTime()
            KeyboardHelper.addToString(text.toString().removeSurrounding("[", "]"),
                beforeText.toString(), false)
            val saveKeyboardDataWork = OneTimeWorkRequestBuilder<SaveKeyboardDataWorker>()
            onSessionChange()
        }
    }

    /** Session has changed, save the current info as KeyboardEvents data class
     * and if the timeslot has changed, also set up a worker that saves info to Firebase.
     * After that, reset values. **/
    private fun onSessionChange() {
        val date = KeyboardHelper.dateFormatter(Date())
        KeyboardHelper.currentTimeSlot = KeyboardHelper.countTimeSlot()
        val keyboardEvent = KeyboardEvents(UUID.randomUUID().toString(),
            KeyboardHelper.countWords(),
            KeyboardHelper.typingTimes,
            KeyboardHelper.deletedChars,
            KeyboardHelper.countErrorRate(),
            KeyboardHelper.timeStampBeginning, System.currentTimeMillis(),
            KeyboardHelper.thisPackage,
            KeyboardHelper.beforeString,
            KeyboardHelper.currentTimeSlot, date
        )
        if (KeyboardHelper.currentTimeSlot != KeyboardHelper.previousTimeSlot) {
            val setUpWork = OneTimeWorkRequestBuilder<KeyboardWorker>()
                .setInputData(workDataOf(
                    "TIMESLOT" to KeyboardHelper.previousTimeSlot
                ))
                .build()
            WorkManager.getInstance(Graph.appContext).enqueue(setUpWork)
        }
        //Add the current event to a list of KeyboardEvents
        KeyboardHelper.dataList.add(keyboardEvent)
        Log.d("KeyboardEvents", "${KeyboardHelper.dataList}")
        resetValues()
    }

    private fun resetValues() {
        KeyboardHelper.previousTimeSlot = KeyboardHelper.currentTimeSlot
        KeyboardHelper.typingTimes = arrayListOf()
        KeyboardHelper.thisPackage = KeyboardHelper.newPackage
        //beforeString = beforeString.substring(beforeString.length - 1)
        KeyboardHelper.beforeString = KeyboardHelper.newString
        KeyboardHelper.newString = ""
            }
    }
