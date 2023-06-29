package com.example.testing

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.testing.utils.*
import com.example.testing.utils.KeyboardHelper.Companion.addToString
import com.example.testing.utils.KeyboardHelper.Companion.beforeString
import com.example.testing.utils.KeyboardHelper.Companion.countErrorRate
import com.example.testing.utils.KeyboardHelper.Companion.countTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.countWords
import com.example.testing.utils.KeyboardHelper.Companion.currentTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.dataList
import com.example.testing.utils.KeyboardHelper.Companion.deletedChars
import com.example.testing.utils.KeyboardHelper.Companion.newPackage
import com.example.testing.utils.KeyboardHelper.Companion.newString
import com.example.testing.utils.KeyboardHelper.Companion.previousTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.sameSession
import com.example.testing.utils.KeyboardHelper.Companion.thisPackage
import com.example.testing.utils.KeyboardHelper.Companion.timeElapsed
import com.example.testing.utils.KeyboardHelper.Companion.timeStampBeginning
import com.example.testing.utils.KeyboardHelper.Companion.typingTimes
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.System.nanoTime
import java.util.*
import kotlin.concurrent.schedule

class MyAccessibilityService : AccessibilityService() {
    private var startTime: Long = 0
    private var endTime: Long = 0

    override fun onDestroy() {
        super.onDestroy()
        val setUpWork = OneTimeWorkRequestBuilder<KeyboardWorker>()
        .setInputData(workDataOf(
            "TIMESLOT" to previousTimeSlot
        )).build()
        WorkManager.getInstance(applicationContext).enqueue(setUpWork)
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService","Service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) { return }
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            /**
            try {
                val setUpKeyboardWork = OneTimeWorkRequestBuilder<SaveKeyboardDataWorker>()
                    .setInputData(workDataOf(
                        "packageName" to event.packageName.toString(),
                        "text" to event.text.toString(),
                        "beforeText" to event.beforeText.toString()
                    ))
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(setUpKeyboardWork)
            } catch (e: Exception) {
                Log.d("MyAccessibilityEvent", "Exception: $e")
            }
            **/
            if (startTime == 0L) {
                //First character in a session, don't add to typing times
                thisPackage = event.packageName.toString()
                timeElapsed = 0.0
                Log.d("KeyboardEvents", "This is the first char of this session")
            } else {
                endTime = nanoTime()
                // Time elapsed in seconds:
                timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                //typingTimes.add(timeElapsed)
                }
            startTime = nanoTime()
            timeStampBeginning = System.currentTimeMillis()
            checkSession(event)
        }
    }

    /** Check if session remains the same.
     * If yes, add written character to string and typing time to an arraylist. **/
    private fun checkSession(event: AccessibilityEvent) {

            if (sameSession(event.packageName.toString(), timeElapsed)) {
            //Same session as before
                saveCharIfNotPassword(event)
            } else { // Session has changed
                newPackage = event.packageName.toString()
                timeElapsed = ((KeyboardHelper.endTime - KeyboardHelper.startTime).toDouble() / 1_000_000_000)
                if (startTime != 0L) {
                    typingTimes.add(timeElapsed)
                }
                saveCharIfNotPassword(event)
                onSessionChange()
            }
    }

    private fun saveCharIfNotPassword(event: AccessibilityEvent) {
        if (!event.isPassword) {
            addToString(event.text.toString().removeSurrounding("[", "]"),
                event.beforeText.toString(), false)
        }
    }

    /** Session has changed, save the current info as KeyboardEvents data class
             * and if the timeslot has changed, also set up a worker that saves info to Firebase.
             * After that, reset values. **/
            private fun onSessionChange() {
                val date = KeyboardHelper.dateFormatter(Date())
                currentTimeSlot = countTimeSlot()
                val keyboardEvent = KeyboardEvents(UUID.randomUUID().toString(), countWords(), typingTimes, deletedChars,
                countErrorRate(), timeStampBeginning, System.currentTimeMillis(), thisPackage,
                beforeString, currentTimeSlot, date
                )
                if (currentTimeSlot != previousTimeSlot) {
                    val setUpWork = OneTimeWorkRequestBuilder<KeyboardWorker>()
                    .setInputData(workDataOf(
                    "TIMESLOT" to previousTimeSlot
                    ))
                    .build()
                    WorkManager.getInstance(applicationContext).enqueue(setUpWork)
                    }
                    //Add the current event to a list of KeyboardEvents
                    dataList.add(keyboardEvent)
                    Log.d("KeyboardEvents", "$dataList")
                    resetValues()
            }

            private fun resetValues() {
                previousTimeSlot = currentTimeSlot
                typingTimes = arrayListOf()
                thisPackage = newPackage
                beforeString = newString
                newString = ""
            }

}

