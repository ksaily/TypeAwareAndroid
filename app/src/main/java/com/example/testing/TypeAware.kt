package com.example.testing

import android.app.Application

class TypeAware : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}