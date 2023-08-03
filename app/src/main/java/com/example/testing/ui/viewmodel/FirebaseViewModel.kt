package com.example.testing.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.testing.fitbit.FitbitApiService
import com.example.testing.data.SleepData
import com.example.testing.data.KeyboardEvents
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
    val dataList = mutableListOf<KeyboardStats>()
    val errorRateList = mutableListOf<Double>()

    fun getSleepData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = FitbitApiService.getSleepData(date)
            _sleepData.postValue(data)
        }
    }

    fun saveSleepDataToFirebase(date: String, data: SleepData, participantId: String) {
        viewModelScope.launch {
            val myRef = Firebase.database.getReference("Data")
            val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()

            // Save data under the current timeslot with an unique id for each
            myRef.child(authId).child(participantId)
                .child(date).child("sleep").setValue(data)
        }
    }

    fun getFromFirebase(date: String, isToday: Boolean) {
            val rootRef = Firebase.database.getReference("Data")
        val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
            val participantId = Utils.readSharedSettingString(
                "p_id",
                "").toString()
            val ref = rootRef.child(authId).child(participantId).child(date)
                .child("keyboardEvents")
            clearAllLists()
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        clearAllLists()
                        try {
                            val children = snapshot.children
                            children.forEach { dataSnapshot ->
                                clearLoopLists()
                                var child = dataSnapshot.children
                                child.forEach {
                                    val speeds =
                                        it.child("typingSpeed").value as Any
                                    if (speeds != null) {
                                        speeds as MutableList<Double>
                                        val avgForOne = speeds.average()
                                        speedsList.add(avgForOne)
                                    }
                                    wordCount = (wordCount + it.child("wordCount").value as Long).toInt()
                                    errorsList.add(it.child("errorAmount").value as Long)
                                    errorRateList.add((it.child("errorRate").value as Number).toDouble())
                                }
                                totalErrList = (totalErrList + errorsList).toMutableList()
                                timeWindow = dataSnapshot.key?.toInt()!!
                                val totalErrorRate = errorRateList.average()
                                totalErr = totalErrList.average()
                                totalSpeed = speedsList.average()
                                //val avgDurationInMinutes = wordCount * (totalSpeed / 60)
                                //val averageWPM = wordCount / avgDurationInMinutes
                                val averageWPM = 60 / totalSpeed
                                var data = KeyboardStats(
                                    date,
                                    timeWindow,
                                    totalErr,
                                    totalSpeed,
                                    totalErrorRate,
                                    averageWPM,
                                )
                                //addToListOfFirebaseData(data)
                                dataList.add(data)
                            }
                            _keyboardData.postValue(dataList)
                        } catch (e: Exception) {
                            Log.d("FirebaseDebug", "Error: $e")
                            // skip this value
                        }
                    } else {
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

    private fun clearAllLists() {
        errorsList.clear()
        speedsList.clear()
        totalSpeedsList.clear()
        speedsList.clear()
        totalAvgErrors.clear()
        totalErrList.clear()
        sessionCount = 0L
        wordCount = 0
        dataList.clear()
    }

    private fun clearLoopLists() {
        errorsList.clear()
        speedsList.clear()
        totalSpeedsList.clear()
        totalAvgErrors.clear()
        totalErrList.clear()
        sessionCount = 0L
        wordCount = 0
    }
}
