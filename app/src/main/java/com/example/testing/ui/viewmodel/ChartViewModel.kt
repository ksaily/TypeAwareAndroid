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


    fun getFromFirebaseToChart(date: String) {
        //val dataFound: Boolean = false
        var sessionCount = 0L
        val rootRef = FirebaseDatabase.getInstance().reference
        val participantId = Utils.readSharedSettingString(
            Graph.appContext,
            "p_id",
            "").toString()
        val ref = rootRef.child("KeyboardEvents").child(participantId).child(date)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val errList = mutableListOf<BarEntry>()
                    val dataList = mutableListOf<BarEntry>()
                    dataFound = true
                    val children = snapshot.children
                    children.forEach { dataSnapshot ->
                        sessionCount = dataSnapshot.childrenCount
                        dataList.add(
                            BarEntry(
                                dataSnapshot.key!!.toFloat(),
                                sessionCount.toFloat()
                            )
                        )
                        val child = dataSnapshot.children
                        child.forEach {
                            var y = it.child("errorAmount").value as Long
                            errList.add(
                                BarEntry(
                                    dataSnapshot.key!!.toFloat(),
                                    y.toFloat()
                                )
                            )
                        }
                    }
                    /**
                    //Take into account missing values
                    for (i in 50..243) {
                        for (j in errList) {
                            if (j.x.toInt() != i) {
                                errList.add(BarEntry(
                                    i.toFloat(), 0f
                                ))
                            }
                        }
                        for (k in dataList) {
                            if (k.x.toInt() != i) {
                                dataList.add(BarEntry(
                                    i.toFloat(), 0f
                                ))
                            }
                        }
                    }**/
                    _chartErrorValues.postValue(errList)
                    _chartSessions.postValue(dataList)
                    Log.d("Firebase", "ChartErrorValues: $chartErrorValues")
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
