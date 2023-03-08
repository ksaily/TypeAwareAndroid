package com.example.testing

import android.app.Application

class AlertnessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}