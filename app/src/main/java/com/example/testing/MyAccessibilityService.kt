package com.example.testing

import android.accessibilityservice.AccessibilityService
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class MyAccessibilityService : AccessibilityService() {
    var KEYBOARD_STATUS: String = "status"
    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        //
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            Log.d("AccesKeyboard", "Keyboard event: " + event.toString())
            /** Create a content provider to store information
             * * val keyboard = ContentValues()
             * keyboard.put("package_name", event.packageName)
             * keyboard.put("event_time", event.eventTime)
             * keyboard.put("new_text", event.text)
             * keyboard.put("before_text", event.beforeText)
             * **/
            Log.d("AccesKeyboard", "New text: " + event.text)
            Log.d("AccesKeyboard", "Before text: " + event.beforeText)
        }
    }

    fun getInfo(context: Context, key: String) {
        TODO("Get info on keyboard click, whether it is a new character or not, return boolean")
    }


}

