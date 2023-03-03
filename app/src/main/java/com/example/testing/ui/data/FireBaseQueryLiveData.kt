package com.example.testing.ui.data

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.testing.utils.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

const val LOG_TAG: String = "FirebaseQueryLiveData"

/**
 * Firebase listeners for changes in data
 */
class FireBaseQueryLiveData: LiveData<DataSnapshot>() {

    companion object {
        lateinit var query: Query

        fun FireBaseQueryLiveData(query: Query) {
            this.query = query
        }

        fun FireBaseQueryLiveData(ref: DatabaseReference) {
            this.query = ref
        }
    }

    private val listener: MyValueEventListener = MyValueEventListener()

    override fun onActive() {
        super.onActive()
        Log.d(LOG_TAG, "onActive")
        query.addValueEventListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        Log.d(LOG_TAG, "onInactive")
        query.removeEventListener(listener)
    }

    inner class MyValueEventListener: ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            setValue(snapshot)
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e(LOG_TAG, "Can't listen to query $query", error.toException() )
        }
    }
}