package com.example.testing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showPushNotification() // implement showing notification in this function
    }

    fun showPushNotification() {
    }


}