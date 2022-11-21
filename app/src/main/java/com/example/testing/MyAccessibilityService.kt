package com.example.testing

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.testing.utils.*
import com.example.testing.utils.KeyboardHelper.Companion.addToString
import com.example.testing.utils.KeyboardHelper.Companion.beforeString
import com.example.testing.utils.KeyboardHelper.Companion.countErrorRate
import com.example.testing.utils.KeyboardHelper.Companion.countTimeSlot
import com.example.testing.utils.KeyboardHelper.Companion.countWords
import com.example.testing.utils.KeyboardHelper.Companion.currentPackage
import com.example.testing.utils.KeyboardHelper.Companion.deletedChars
import com.example.testing.utils.KeyboardHelper.Companion.errorRate
import com.example.testing.utils.KeyboardHelper.Companion.newPackage
import com.example.testing.utils.KeyboardHelper.Companion.sameSession
import com.example.testing.utils.KeyboardHelper.Companion.timeElapsed
import com.example.testing.utils.KeyboardHelper.Companion.timeSlots
import com.example.testing.utils.KeyboardHelper.Companion.timeStampBeginning
import com.example.testing.utils.KeyboardHelper.Companion.typingTimes
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.System.nanoTime
import java.util.*

class MyAccessibilityService : AccessibilityService() {
    var startTime: Long = nanoTime()
    var endTime: Long = 0

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //var dbHelper = DataBaseHelperImpl(DatabaseBuilder.getInstance(applicationContext))
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (startTime == 0L) {
                //First character in a session, don't add to typing times
                currentPackage = event.packageName.toString()
                KeyboardHelper.timeStampBeginning = System.currentTimeMillis()
            } else {
                endTime = nanoTime()
                // Time elapsed in seconds:
                KeyboardHelper.timeElapsed = ((endTime - startTime).toDouble() / 1_000_000_000)
                KeyboardHelper.typingTimes.add(KeyboardHelper.timeElapsed)
            }
            startTime = nanoTime()
            checkSession(event)
        }
    }

    private fun saveToFirebase(path: String, timeslot: Int, events: KeyboardEvents) {
        val database = Firebase.database("https://health-app-9c151-default-rtdb.europe-west1.firebasedatabase.app")
        val myRef = database.getReference("KeyboardEvents")
        myRef.child("events").child(timeslot.toString()).child(path).setValue(events)
        Log.d("KeyboardEvents", "Info saved to firebase")
    }

    private fun checkSession(event: AccessibilityEvent) {
        if (!sameSession(event.packageName.toString(), KeyboardHelper.timeElapsed)) {
            onSessionChange()
        } else {
            addToString(event.text.toString().removeSurrounding("[", "]"),
            event.beforeText.toString())
        }
    }

    private fun onSessionChange() {
        var id= UUID.randomUUID().toString()
        val keyboardEvent = KeyboardEvents(id, countWords(), typingTimes, deletedChars,
        countErrorRate(), timeStampBeginning, System.currentTimeMillis(), currentPackage,
        beforeString, countTimeSlot(), Calendar.getInstance().get(Calendar.DATE))
        saveToFirebase(id, timeSlots, keyboardEvent)
        resetValues()
    }

    private fun resetValues() {
        startTime; timeElapsed; deletedChars = 0
        typingTimes = arrayListOf()
        currentPackage = newPackage
        var startNewString = beforeString.substring(beforeString.length - 1)
        beforeString = startNewString
    }

}

