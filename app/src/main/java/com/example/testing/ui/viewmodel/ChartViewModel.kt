package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testing.Graph
import com.example.testing.fitbit.FitbitApiService.Companion.authorizeRequestToken
import com.example.testing.fitbit.FitbitApiService.Companion.getRefreshToken
import com.example.testing.ui.data.KeyboardChart
import com.example.testing.ui.data.SleepData
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
    private val sleepDataList = ArrayList<SleepDataForChart>()

    fun getFirebaseData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getFromFirebaseToChart(date)
        }
    }


    fun getFromFirebaseToChart(date: String) {
        //val dataFound: Boolean = false
        var sessionCount = 0L
        val rootRef = FirebaseDatabase.getInstance().reference
        val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
        val participantId = Utils.readSharedSettingString(
            "p_id",
            "").toString()
        val ref = rootRef.child("Data").child(authId).child(participantId).child(date)
            .child("keyboardEvents")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val errList = mutableListOf<BarEntry>()
                    val dataList = mutableListOf<BarEntry>()
                    val speedList = mutableListOf<BarEntry>()
                    var wordCount = 0
                    dataFound = true
                    val averageWPMList = mutableListOf<Double>()
                    val averageWPM = Double
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
                        try {
                            child.forEach {
                                var y = it.child("errorAmount").value as Long
                                var speeds = it.child("typingSpeed").value
                                if (speeds != null) {
                                    speeds as MutableList<Double>
                                    var avgForOne = speeds.average()
                                    speedsAvgList.add(avgForOne)
                                }
                                errorsAvgList.add(y)
                                sessionCount += 1
                                wordCount = (wordCount + it.child("wordCount").value as Long).toInt()
                                Log.d("ChartViewModel", "ErrorAmount: $y")
                            }
                        } catch (e: Exception) {
                            Log.d("Firebase", "Error: $e ")
                        }

                        val avgDurationInMinutes = wordCount * (speedsAvgList.average() / 60)
                        val averageWPM = wordCount / avgDurationInMinutes

                        for (i in iterErrList) {
                            val timewindow = dataSnapshot.key?.toInt()
                            if (timewindow!! < 144) {
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
                                    averageWPM.toFloat()
                                )
                            }

                        }

                        }
                        _chartErrorValues.postValue(iterErrList)
                        _chartSessions.postValue(dataList)
                        _chartSpeedValues.postValue(iterSpeedList)
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

    fun getSleepDataFromThisWeek(startDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sleepDataList.clear()
            dates.clear()
            var previousDay = startDate
            for (i in 0 .. 6) {
                sleepDataList.add(SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f))))
            }

            for (i in 0..6) {
                dates.add(previousDay)
                previousDay = Utils.getPreviousDateString(previousDay)
            }
            dates.reverse()

            val accessToken = Utils.readSharedSettingString("access_token", "")
            FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"

            val startDateFitbit = Utils.formatForFitbit(dates.first())
            val endDateFitbit = Utils.formatForFitbit(dates.last())
            Log.d("Dates reversed", dates.toString())

            val url = "/sleep/date/$startDateFitbit/$endDateFitbit.json"
            try {
                val (_, response, result) = url.httpGet().header(
                    "Authorization" to "Bearer $accessToken"
                ).responseString()
                println(response)

                val (sleepData, error) = result
                if (response.isSuccessful) {
                    val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                    val jsonArray = jsonObject.optJSONArray("sleep")
                    println(jsonArray)
                    if (jsonArray != null && jsonArray.length() > 0) {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            Log.d("Obj", obj.toString())
                            val dateOfSleep = obj.getString("dateOfSleep")
                            Log.d("DateOfSleep", dateOfSleep.toString())
                            val sleepLogDay = Utils.formatDateStringFromFitbit(dateOfSleep)
                            Log.d("levels", obj.getJSONObject("levels").toString())
                            val summary = obj.getJSONObject("levels").getJSONObject("summary")
                            Log.d("levels", summary.toString())
                            val deepSleep = summary.getJSONObject("deep").getInt("minutes")
                            val lightSleep = summary.getJSONObject("light").getInt("minutes")
                            val remSleep = summary.getJSONObject("rem").getInt("minutes")
                            val wakeSleep = summary.getJSONObject("wake").getInt("minutes")
                            Log.d("deep", deepSleep.toString())
                            Log.d("light", lightSleep.toString())
                            Log.d("rem", remSleep.toString())
                            Log.d("wake", wakeSleep.toString())


                            val index = dates.indexOf(sleepLogDay)
                            Log.d("Index", "$index Day $sleepLogDay")
                            if (index != -1) {
                                sleepDataList[index] = SleepDataForChart(
                                    Utils.formatDateForChart(sleepLogDay),
                                    BarEntry(index.toFloat(), floatArrayOf(
                                        deepSleep.toFloat(),
                                        lightSleep.toFloat(),
                                        remSleep.toFloat(),
                                        wakeSleep.toFloat()
                                    ))
                                )
                            }
                        }

                        Log.d("SleepDataValuesList:", sleepDataList.toString())
                        Log.d("SleepDataValues", "Post")
                        _sleepDataValues.postValue(sleepDataList)
                    }
                 else {
                    _sleepDataValues.postValue(ArrayList<SleepDataForChart>())
                    }
                }
                else if (response.statusCode == 401) {
                    val code = Utils.readSharedSettingString("authorization_code", "")
                    val state = Utils.readSharedSettingString("state", "")
                    if (code!!.isNotEmpty() && state!!.isNotEmpty() && !authAttempted) {
                        Log.d("GetSleepDataFailure", "Re-authorizing")
                        getRefreshToken(code, state)
                        authAttempted = true
                        getSleepDataFromThisWeek(startDate)
                    } else {
                        authAttempted = false
                    }
                }
        } catch (e: Exception) {
            Log.d("Error:", "$e")
        }
        }
    }


    fun emptySleepDataList() {
        sleepDataList.clear()
    }

    private fun getSleepDataToCharts(date: String) {
        val startDateFitbit = Utils.formatForFitbit(date)
        val endDate = Utils.getDateWeekFromNow(date)
        val endDateFitbit = Utils.formatForFitbit(endDate)
        try {
            val accessToken = Utils.readSharedSettingString("access_token", "")
            Log.d("GetSleepDataFromThisWeek", "week's date: $startDateFitbit")
            FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
            val url = "/sleep/date/$startDateFitbit.json"

            val (_, response, result) = url.httpGet().header(
                "Authorization" to
                        "Bearer $accessToken"
            ).responseString()
            val (sleepData, error) = result
            Log.d("GetSleepData", "sleepData: $sleepData")
            if (response.isSuccessful) {
                val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                val jsonArray = jsonObject.getJSONArray("sleep")
                if (jsonArray.isNull(0)) {
                    //No data for this date
                    sleepDataList[0] = (
                            SleepDataForChart(Utils.formatDateForChart(date), BarEntry(0.toFloat(), 0f))
                            )
                }
                else {
                    val obj = jsonArray.getJSONObject(0)
                    val dateOfSleep = obj.getString("dateOfSleep")
                    val sleepLogDay = Utils.formatDateStringFromFitbit(dateOfSleep)
                    val summary = jsonObject.getJSONObject("summary").getJSONObject("stages")
                    val deepSleep = summary.getString("deep")
                    val lightSleep = summary.getString("light")
                    val remSleep = summary.getString("rem")
                    val wakeSleep = summary.getString("wake")
                    Log.i("Sleep", "Date of sleep : $sleepLogDay")
                    sleepDataList[0] =
                        SleepDataForChart(Utils.formatDateForChart(sleepLogDay), BarEntry(0.toFloat(),
                            floatArrayOf(deepSleep.toFloat(),
                                lightSleep.toFloat(),
                                remSleep.toFloat(),
                                wakeSleep.toFloat())))
                    authAttempted = false
                }
            } else if (response.statusCode == 401) {
                var code = Utils.readSharedSettingString("authorization_code", "")
                var state = Utils.readSharedSettingString("state", "")
                return if (code!!.isNotEmpty() && state!!.isNotEmpty() && !authAttempted) {
                    Log.d("GetSleepDataFailure", "Re-authorizing")
                    getRefreshToken(code, state)
                    authAttempted = true
                    getSleepDataFromThisWeek(date)
                } else {
                    authAttempted = false
                }
            }
        } catch (e: Exception) {
            authAttempted = false
            Log.d("Fitbit Authorization(Get Sleep Data)", "Error: $e")
        }
    }


}

data class SleepDataForChart(
    val date: String,
    val entry: BarEntry
)
