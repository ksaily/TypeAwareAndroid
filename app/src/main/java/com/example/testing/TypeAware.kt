package com.example.testing

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TypeAware : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.database.setPersistenceEnabled(true)
        Graph.provide(this)
    }
}