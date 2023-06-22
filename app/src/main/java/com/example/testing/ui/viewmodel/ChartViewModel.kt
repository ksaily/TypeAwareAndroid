package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testing.Graph
import com.example.testing.fitbit.FitbitApiService.Companion.authorizeRequestToken
import com.example.testing.ui.data.KeyboardChart
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpGet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

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

    private val _chartSpeedValues = MutableLiveData<List<BarEntry>>()
    val chartSpeedValues: LiveData<List<BarEntry>>
        get() = _chartSpeedValues

    private val _sleepDataValues = MutableLiveData<List<SleepDataForChart>>()
    val sleepDataValues: LiveData<List<SleepDataForChart>>
        get() = _sleepDataValues

    var dataFound: Boolean = false
    var chartSelected: Int = 0 // 0 if errors, 1 if speed
    var authAttempted = false
    val dates = arrayListOf<String>()
    private val sleepDataList = ArrayList<SleepDataForChart>(7)


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
                    try {
                        val errList = mutableListOf<BarEntry>()
                        val dataList = mutableListOf<BarEntry>()
                        val speedList = mutableListOf<BarEntry>()
                        dataFound = true
                        var sessionCount = 0
                        val errorsAvgList = mutableListOf<Long>()
                        val speedsAvgList = mutableListOf<Double>()
                        val iterErrList = mutableListOf<BarEntry>()
                        val iterSpeedList = mutableListOf<BarEntry>()
                        for (i in 0..144) {
                            dataList.add(BarEntry(i.toFloat(), 0f))
                            iterErrList.add(BarEntry(i.toFloat(), 0f))
                            iterSpeedList.add(BarEntry(i.toFloat(), 0f))
                        }
                        val children = snapshot.children
                        children.forEach { dataSnapshot ->
                            val child = dataSnapshot.children
                            child.forEach {
                                var y = it.child("errorAmount").value as Long
                                var speeds = it.child("typingSpeed").value as MutableList<Double>
                                if (speeds != null) {
                                    var avgForOne = speeds.average()
                                    speedsAvgList.add(avgForOne)
                                }
                                errorsAvgList.add(y)
                                sessionCount += 1
                                Log.d("ChartViewModel", "ErrorAmount: $y")
                            }

                            for (i in iterErrList) {
                                val timewindow = dataSnapshot.key?.toInt()
                                if (timewindow!! < 144) {
                                    Log.d("ChartViewModel", "Timewindow: $timewindow")
                                    iterErrList[timewindow!!] = BarEntry(
                                        timewindow.toFloat(),
                                        errorsAvgList.average().toFloat()
                                    )
                                    dataList[timewindow!!] = BarEntry(
                                        timewindow.toFloat(),
                                        sessionCount.toFloat()
                                    )
                                    iterSpeedList[timewindow!!] = BarEntry(
                                        timewindow.toFloat(),
                                        (60 / speedsAvgList.average()).toFloat()
                                    )
                                }


                            }

                        }
                        _chartErrorValues.postValue(iterErrList)
                        _chartSessions.postValue(dataList)
                        _chartSpeedValues.postValue(iterSpeedList)
                        Log.d("Firebase", "ChartErrorValues: $")
                        Log.d("Firebase", "ChartSessions: $chartSessions")
                        //_keyboardStats.postValue(dataList)
                } catch (e: Exception) {
                    Log.d("Firebase", "Error: $e ")
                    }
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

    fun getSleepDataFromThisWeek(startDate: String) {
        //val iterList = ArrayList<SleepDataForChart>(7)
        //sleepDataList.clear()
        var previousDay = startDate
        viewModelScope.launch(Dispatchers.IO) {
            for (i in 6 downTo 0) {
                getSleepDataToCharts(previousDay, i)
                previousDay = Utils.getPreviousDateString(previousDay)
            }
            Log.d("SleepDataValuesList:", sleepDataList.toString())
            _sleepDataValues.postValue(sleepDataList)
        }
    }

    fun emptySleepDataList() {
        sleepDataList.clear()
    }

    private fun getSleepDataToCharts(date: String, x: Int) {
        val dateFitbit = Utils.formatForFitbit(date)
        try {
            val accessToken = Utils.readSharedSettingString(Graph.appContext, "access_token", "")
            Log.d("GetSleepDataFromThisWeek", "week's date: $dateFitbit")
            FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
            val url = "/sleep/date/$dateFitbit.json"

            val (_, response, result) = url.httpGet().header(
                "Authorization" to
                        "Bearer $accessToken"
            ).responseString()
            val (sleepData, error) = result
            Log.d("GetSleepData", "sleepData: $sleepData")
            if (response.isSuccessful) {
                if (sleepData == null) {
                    //No data for this date
                    sleepDataList[x] = (
                        SleepDataForChart(Utils.formatDateForChart(date), BarEntry(x.toFloat(), 0f))
                    )
                }
                else {
                    val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                    val jsonArray = jsonObject.getJSONArray("sleep")
                    val obj = jsonArray.getJSONObject(0)
                    val dateOfSleep = obj.getString("dateOfSleep")
                    val sleepLogDay = Utils.formatDateStringFromFitbit(dateOfSleep)
                    val summary = jsonObject.getJSONObject("summary").getJSONObject("stages")
                    val deepSleep = summary.getString("deep")
                    val lightSleep = summary.getString("light")
                    val remSleep = summary.getString("rem")
                    val wakeSleep = summary.getString("wake")
                    Log.i("Sleep", "Date of sleep : $sleepLogDay")
                    sleepDataList[x] =
                        SleepDataForChart(Utils.formatDateForChart(sleepLogDay), BarEntry(x.toFloat(),
                            floatArrayOf(deepSleep.toFloat(),
                                lightSleep.toFloat(),
                                remSleep.toFloat(),
                                wakeSleep.toFloat())))
                    authAttempted = false
                }
            } else if (response.statusCode == 401) {
                var code = Utils.readSharedSettingString(
                    Graph.appContext,
                    "authorization_code", ""
                )
                var state = Utils.readSharedSettingString(
                    Graph.appContext, "state", ""
                )
                return if (code!!.isNotEmpty() && state!!.isNotEmpty() && !authAttempted) {
                    Log.d("GetSleepDataFailure", "Re-authorizing")
                    authorizeRequestToken(code!!, state!!)
                    authAttempted = true
                    getSleepDataFromThisWeek(date)
                } else {
                    authAttempted = false
                }
            } else {
                emptySleepDataList()
            }
        } catch (e: Exception) {
            authAttempted = false
            Log.d("Fitbit Authorization(Get Sleep Data)", "Error: $e")
            emptySleepDataList()
        }
    }


}

data class SleepDataForChart(
    val date: String,
    val entry: BarEntry
)
