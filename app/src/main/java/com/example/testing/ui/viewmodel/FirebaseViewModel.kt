package com.example.testing.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.example.testing.Graph
import com.example.testing.utils.KeyboardEvents
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * ViewModel for returning Firebase data when updated
 */
class FirebaseViewModel(application: Application): AndroidViewModel(application) {

    private val _keyboardData = MutableLiveData<List<KeyboardStats>>()
    val keyboardData: LiveData<List<KeyboardStats>>
        get() = _keyboardData

    private val _keyboardEvents = MutableLiveData(ArrayList<KeyboardEvents>())
    val keyboardEvents: LiveData<ArrayList<KeyboardEvents>>
        get() = _keyboardEvents

    private val _chartErrorValues = MutableLiveData(ArrayList<BarEntry>())
    val chartErrorValues: LiveData<ArrayList<BarEntry>>
        get() = _chartErrorValues

    private val _chartSessions = MutableLiveData(ArrayList<BarEntry>())
    val chartSessions: LiveData<ArrayList<BarEntry>>
        get() = _chartSessions


    var errorsList: MutableList<Long> = mutableListOf()
    var totalErrList: MutableList<Long> = mutableListOf()
    var totalSpeedsList: MutableList<MutableList<Double>> = mutableListOf()
    var totalAvgErrors: java.util.ArrayList<Long> = arrayListOf()
    var timeWindow: Int = 0
    var totalErr: Double = 0.0
    var totalSpeed: Double = 0.0
    var speedsList: MutableList<Double> = mutableListOf()
    var sessionCount: Long = 0L
    var dataFound: Boolean = false

/**
    fun addToListOfFirebaseData(data: KeyboardStats) {
        _keyboardData.value?.add(data)
    }

    fun clearListOfFirebaseData() {
        _keyboardData.value?.clear()

    }**/

    fun getFromFirebase(date: String, isToday: Boolean) {
        val rootRef = FirebaseDatabase.getInstance().reference
        val participantId = Utils.readSharedSettingString(
            Graph.appContext,
            "p_id",
            "").toString()
        val ref = rootRef.child("KeyboardEvents").child(participantId).child(date)
        Log.d("FirebaseDebug", ref.toString())
        errorsList.clear()
        speedsList.clear()
        totalSpeedsList.clear()
        totalErrList.clear()
        val errorRateList = mutableListOf<Double>()
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dataList = mutableListOf<KeyboardStats>()
                if (snapshot.exists()) {
                    val children = snapshot.children
                    children.forEach { dataSnapshot ->
                        var child = dataSnapshot.children
                        child.forEach {
                            var speeds = it.child("typingSpeed").value as MutableList<Double>
                            Log.d("FirebaseDebug", "Speed: $speeds")
                            if (speeds != null) {
                                var avgForOne = speeds.average()
                                Log.d("FirebaseDebug", "Avg speed for one: $avgForOne")
                                speedsList.add(avgForOne)
                            }
                            errorsList.add(it.child("errorAmount").value as Long)
                            errorRateList.add((it.child("errorRate").value as Number).toDouble())
                            //Add the average for one instance to a new list
                            Log.d("FirebaseDebug", "SpeedsList: $speedsList")
                            Log.d("FirebaseDebug", "ErrorsList: $errorsList")
                            Log.d("FirebaseDebug", "ErrorRateList: $errorRateList")
                        }
                        totalErrList = (totalErrList + errorsList).toMutableList()
                        //totalSpeedsList.add(speedsList.toMutableList())
                        timeWindow = dataSnapshot.key?.toInt()!!
                        //Log.d("FirebaseDebug", "TotalSpeedsList: $totalSpeedsList")
                        Log.d("FirebaseDebug", "TotalErrorsList: ${totalErrList}")
                        //avgSpeed = countAvgSpeed(totalAvgSpeed)
                        //var data = KeyboardStats(date, dataSnapshot.key, avgErrors, avgSpeed)
                        //println(data)
                        val totalErrorRate = errorRateList.average()
                        totalErr = totalErrList.average()
                        Log.d("FirebaseDebug", "totalErr: $totalErr")
                        totalSpeed = speedsList.average()
                        Log.d("FirebaseDebug", "TotalSpeed: $totalSpeed")
                        var data = KeyboardStats(
                            date,
                            timeWindow,
                            totalErr,
                            totalSpeed,
                            totalErrorRate)
                        println(data)
                        //addToListOfFirebaseData(data)
                        dataList.add(data)
                    }
                    _keyboardData.postValue(dataList)
                    Log.d("Firebase", "Data fetched from firebase")
                    println(keyboardData.value)

                } else{
                    Log.d("Firebase", "No data found")
                    _keyboardData.postValue(mutableListOf())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Firebase", error.message)
            }
        }
        if (!isToday) {
            ref.addListenerForSingleValueEvent(valueEventListener)
        } else {
            ref.addValueEventListener(valueEventListener)
        }
    }

    fun getFromFirebaseToChart(date: String): Boolean {
        //val dataFound: Boolean = false
        val rootRef = FirebaseDatabase.getInstance().reference
        val participantId = Utils.readSharedSettingString(
            Graph.appContext,
            "p_id",
            "").toString()
        val ref = rootRef.child(participantId).child(date)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    dataFound = true
                    val children = snapshot.children
                    children.forEach { dataSnapshot ->
                        sessionCount = dataSnapshot.childrenCount
                        chartSessions.value?.add(
                            BarEntry(
                                snapshot.key!!.toFloat(),
                                sessionCount.toFloat()
                            )
                        )
                        val child = dataSnapshot.children
                        child.forEach {
                            var y = it.child("errorAmount").value as Int
                            chartErrorValues.value?.add(
                                BarEntry(
                                    snapshot.key!!.toFloat(),
                                    y.toFloat()
                                )
                            )
                        }

                    }
                    Log.d("Firebase", "ChartErrorValues: $chartErrorValues")
                    Log.d("Firebase", "ChartSessions: $chartSessions")
                } else {
                    dataFound = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                dataFound = false
            }
        }
        ref.addValueEventListener(valueEventListener)
        return dataFound
    }
}