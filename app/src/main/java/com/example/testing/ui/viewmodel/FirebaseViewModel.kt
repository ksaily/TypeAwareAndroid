package com.example.testing.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.testing.Graph
import com.example.testing.fitbit.FitbitApiService
import com.example.testing.ui.data.SleepData
import com.example.testing.utils.KeyboardEvents
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for returning Firebase data when updated
 */
class FirebaseViewModel(application: Application): AndroidViewModel(application) {

    private val _keyboardData = MutableLiveData<List<KeyboardStats>>()
    val keyboardData: LiveData<List<KeyboardStats>>
        get() = _keyboardData

    private val _sleepData = MutableLiveData<SleepData>()
    val sleepData: LiveData<SleepData>
        get() = _sleepData

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
    var wordCount: Int = 0

    fun getSleepData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = FitbitApiService.getSleepData(date)
            _sleepData.postValue(data)
        }
    }

    fun saveSleepDataToFirebase(date: String, data: SleepData, participantId: String) {
        viewModelScope.launch {
            val myRef = Firebase.database.getReference("Data")
            // Save data under the current timeslot with an unique id for each
            myRef.child(participantId.toString())
                .child(date).child("sleep").setValue(data)
        }
    }

    fun getFromFirebase(date: String, isToday: Boolean) {
            val rootRef = Firebase.database.getReference("Data")
            val participantId = Utils.readSharedSettingString(
                Graph.appContext,
                "p_id",
                "").toString()
            val ref = rootRef.child(participantId).child(date)
                .child("keyboardEvents")
            clearLists()
            val errorRateList = mutableListOf<Double>()
            val valueEventListener = object : ValueEventListener {
                val dataList = mutableListOf<KeyboardStats>()
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                            val children = snapshot.children
                            children.forEach { dataSnapshot ->
                                var child = dataSnapshot.children
                                child.forEach {
                                    try {
                                        val speeds =
                                            it.child("typingSpeed").value as MutableList<*>
                                        Log.d("FirebaseDebug", "Speed: $speeds")
                                        if (speeds.isNotEmpty()) {
                                            speeds as MutableList<Double>
                                            var avgForOne = speeds.average()
                                            Log.d("FirebaseDebug", "Avg speed for one: $avgForOne")
                                            speedsList.add(avgForOne)
                                        }
                                        wordCount = (wordCount + it.child("wordCount").value as Long).toInt()
                                        errorsList.add(it.child("errorAmount").value as Long)
                                        errorRateList.add((it.child("errorRate").value as Number).toDouble())
                                        //Add the average for one instance to a new list
                                        Log.d("FirebaseDebug", "SpeedsList: $speedsList")
                                        Log.d("FirebaseDebug", "ErrorsList: $errorsList")
                                        Log.d("FirebaseDebug", "ErrorRateList: $errorRateList")
                                    } catch (e: Exception) {
                                        Log.d("FirebaseDebug", "Error: $e")
                                        // skip this value
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
                                    totalErrorRate,
                                    wordCount)
                                println(data)
                                //addToListOfFirebaseData(data)
                                dataList.add(data)
                            }
                            Log.d("FirebaseDatalist", dataList.toString())
                            _keyboardData.postValue(dataList)
                            Log.d("Firebase", "Data fetched from firebase")
                        }
                    }
                    else {
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

    private fun clearLists() {
        errorsList.clear()
        speedsList.clear()
        totalSpeedsList.clear()
        totalErrList.clear()
        wordCount = 0
    }
}
