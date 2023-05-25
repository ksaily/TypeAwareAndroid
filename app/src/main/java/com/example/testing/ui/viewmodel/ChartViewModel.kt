package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.testing.Graph
import com.example.testing.ui.data.KeyboardChart
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChartViewModel: ViewModel() {

    private val _keyboardStats = MutableLiveData<List<KeyboardChart>>()
    val keyboardStats: LiveData<List<KeyboardChart>>
        get() = _keyboardStats

    private val _chartErrorValues = MutableLiveData<List<BarEntry>>()
    val chartErrorValues: LiveData<List<BarEntry>>
        get() = _chartErrorValues

    private val _chartSessions = MutableLiveData<List<BarEntry>>()
    val chartSessions: LiveData<List<BarEntry>>
        get() = _chartSessions

    var dataFound: Boolean = false
    var chartSelected: Int = 0 // 0 if errors, 1 if speed


    fun getFromFirebaseToChart(date: String) {
        //val dataFound: Boolean = false
        var sessionCount = 0L
        val rootRef = FirebaseDatabase.getInstance().reference
        val participantId = Utils.readSharedSettingString(
            Graph.appContext,
            "p_id",
            "").toString()
        val ref = rootRef.child("Data").child(participantId).child(date)
            .child("keyboardEvents")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val errList = mutableListOf<BarEntry>()
                    val dataList = mutableListOf<BarEntry>()
                    dataFound = true
                    var sessionCount = 0
                    val errorsAvgList = mutableListOf<Long>()
                    val iterErrList = mutableListOf<BarEntry>()
                    for (i in 0..144) {
                        dataList.add(BarEntry(i.toFloat(), 0f))
                        iterErrList.add(BarEntry(i.toFloat(), 0f))
                    }
                    val children = snapshot.children
                    children.forEach { dataSnapshot ->
                        val child = dataSnapshot.children
                        child.forEach {
                            var y = it.child("errorAmount").value as Long
                            errorsAvgList.add(y)
                            sessionCount += 1
                            Log.d("ChartViewModel", "ErrorAmount: $y")
                        }

                        for (i in iterErrList) {
                            val timewindow = dataSnapshot.key?.toInt()
                            Log.d("ChartViewModel", "Timewindow: $timewindow")
                            iterErrList[timewindow!!] = BarEntry(
                                timewindow.toFloat(),
                                errorsAvgList.average().toFloat()
                            )
                            dataList[timewindow!!] = BarEntry(
                                timewindow.toFloat(),
                                sessionCount.toFloat()
                            )

                        }

                    }
                    _chartErrorValues.postValue(iterErrList)
                    _chartSessions.postValue(dataList)
                    Log.d("Firebase", "ChartErrorValues: $")
                    Log.d("Firebase", "ChartSessions: $chartSessions")
                    //_keyboardStats.postValue(dataList)
                } else {
                    dataFound = false
                    Log.d("FirebaseChart", "No data found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("FirebaseChart", "No data found")
                dataFound = false
            }
        }
        ref.addValueEventListener(valueEventListener)
    }
}
