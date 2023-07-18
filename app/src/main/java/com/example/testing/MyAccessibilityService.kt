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
import com.example.testing.utils.KeyboardHelper.Companion.checkSameSession
import com.example.testing.utils.KeyboardHelper.Companion.countErrorRate
import com.example.testing.utils.KeyboardHelper.Companion.countTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.countWords
import com.example.testing.utils.KeyboardHelper.Companion.currentTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.dataList
import com.example.testing.utils.KeyboardHelper.Companion.deletedChars
import com.example.testing.utils.KeyboardHelper.Companion.newPackage
import com.example.testing.utils.KeyboardHelper.Companion.newString
import com.example.testing.utils.KeyboardHelper.Companion.previousTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.thisPackage
import com.example.testing.utils.KeyboardHelper.Companion.timeElapsed
import com.example.testing.utils.KeyboardHelper.Companion.timeStampBeginning
import com.example.testing.utils.KeyboardHelper.Companion.typingTimes
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.concurrent.schedule

class MyAccessibilityService : AccessibilityService() {
    private var startTimeBetweenChars: Long = 0L
    private var endTimeBetweenChars: Long = 0L
    private var timeElapsedBetweenChars: Double = 0.0

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
            if (startTimeBetweenChars == 0L) {
                //First character in a session, don't add to typing times
                thisPackage = event.packageName.toString()
                timeElapsedBetweenChars = 0.0
                Log.d("KeyboardEvents", "This is the first char of this session")
            } else {
                endTimeBetweenChars = System.nanoTime()
                timeElapsedBetweenChars = ((endTimeBetweenChars - startTimeBetweenChars).toDouble() / 1_000_000_000)
            }
            startTimeBetweenChars = System.nanoTime()
            timeStampBeginning = System.currentTimeMillis()
            checkSession(event)
        }
    }

    /** Check if session remains the same.
     * If yes, add written character to string and typing time to an arraylist. **/
    private fun checkSession(event: AccessibilityEvent) {

            if (checkSameSession(event.packageName.toString(), timeElapsedBetweenChars)) {
            //Same session as before
                saveCharIfNotPassword(event, true)
            } else { // Session has changed
                newPackage = event.packageName.toString()
                saveCharIfNotPassword(event, false)
                onSessionChange()
            }
    }

    private fun saveCharIfNotPassword(event: AccessibilityEvent, sameSession: Boolean) {
        if (!event.isPassword) {
            addToString(event.text.toString().removeSurrounding("[", "]"),
                event.beforeText.toString(), sameSession)
        }
    }

    /** Session has changed, save the current info as KeyboardEvents data class
             * and if the timeslot has changed, also set up a worker that saves info to Firebase.
             * After that, reset values. **/
            private fun onSessionChange() {
                if (beforeString.isNullOrEmpty() && typingTimes.isEmpty()) {
                    resetValues()
                } else {
                    val date = KeyboardHelper.dateFormatter(Date())
                    currentTimeSlot = countTimeSlot()
                    val keyboardEvent = KeyboardEvents(UUID.randomUUID().toString(),
                        countWords(),
                        typingTimes,
                        deletedChars,
                        countErrorRate(),
                        timeStampBeginning,
                        System.currentTimeMillis(),
                        thisPackage,
                        beforeString,
                        currentTimeSlot,
                        date
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
            }

            private fun resetValues() {
                previousTimeSlot = currentTimeSlot
                typingTimes = arrayListOf()
                thisPackage = newPackage
                beforeString = newString
                newString = ""
            }

}

