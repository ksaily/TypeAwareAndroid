package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testing.fitbit.FitbitApiService.Companion.getRefreshToken
import com.example.testing.data.KeyboardChart
import com.example.testing.utils.Utils
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpGet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

class ChartViewModel: ViewModel() {

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
    var dataList = mutableListOf<BarEntry>()
    var errorsAvgList = mutableListOf<Double>()
    var speedsAvgList = mutableListOf<Double>()
    var iterErrList = mutableListOf<BarEntry>()
    var iterSpeedList = mutableListOf<BarEntry>()
    var sessionCount: Long = 0L
    var wordCount = 0
    var averageWPM : Double = 0.0

    fun getFirebaseData(date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getFromFirebaseToChart(date)
        }
    }

    fun clearChartArrays() {
        _chartErrorValues.postValue(listOf())
        _chartSessions.postValue(listOf())
        _chartSpeedValues.postValue(listOf())
        _sleepDataValues.postValue(listOf())
    }

    private fun clearAllLists() {
        dataList.clear()
        iterErrList.clear()
        iterSpeedList.clear()
        for (i in 0..143) {
            dataList.add(BarEntry(i.toFloat(), 0f))
            iterErrList.add(BarEntry(i.toFloat(), 0f))
            iterSpeedList.add(BarEntry(i.toFloat(), 0f))
        }
    }

    private fun saveSleepDataToFirebase(date: String, data: Map<String, Any>) {
        viewModelScope.launch {
            val myRef = Firebase.database.getReference("Data")
            val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
            val participantId = Utils.readSharedSettingString("p_id", "").toString()

            // Save data under the current timeslot with an unique id for each
            myRef.child(authId).child(participantId)
                .child(date).child("sleep").setValue(data)
        }
    }

    private fun clearLoopLists() {
        sessionCount = 0
        wordCount = 0
        dataFound = true
        averageWPM = 0.0
        errorsAvgList.clear()
        speedsAvgList.clear()
    }


    fun getFromFirebaseToChart(date: String) {
        //val dataFound: Boolean = false
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
                    clearAllLists()
                    val children = snapshot.children
                    children.forEach { dataSnapshot ->
                        clearLoopLists()
                        val child = dataSnapshot.children
                        child.forEach {
                            try {
                                val y = it.child("errorRate").value as Any
                                if (y is Double) {
                                    errorsAvgList.add(y.toDouble())
                                }
                                val speeds = it.child("typingSpeed").value as Any
                                if (speeds != null) {
                                    speeds as MutableList<Double>
                                    var avgForOne = speeds.average()
                                    speedsAvgList.add(avgForOne)
                                }
                                //errorsAvgList.add(y)
                                sessionCount += 1
                                wordCount = (wordCount + it.child("wordCount").value as Long).toInt()
                            } catch (e: Exception) {
                                Log.d("FirebaseError", "$e")
                            }
                        }
                        if (checkDoubleNotNull(speedsAvgList.average())) {
                            averageWPM = 60 / speedsAvgList.average() }
                        else { averageWPM = 0.0
                        }
                        val averageError: Double
                        averageError = if (checkDoubleNotNull(errorsAvgList.average())) {
                            errorsAvgList.average()
                        } else {
                            0.0
                        }


                        //val avgDurationInMinutes = wordCount * (speedsAvgList.average() / 60)
                        //val averageWPM = wordCount / avgDurationInMinutes

                        val timewindow = dataSnapshot.key?.toInt()
                        for (i in iterErrList) {
                            //val timewindow = dataSnapshot.key?.toInt()?.plus(1)
                            if (timewindow!! < iterErrList.size ) {
                                iterErrList[timewindow!!] = BarEntry(
                                    timewindow.toFloat(),
                                    averageError.toFloat()
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

    private fun checkDoubleNotNull(double: Double): Boolean {
        return (double.isFinite())
    }

    fun getSleepDataFromThisWeek(startDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sleepDataList.clear()
                dates.clear()
                var previousDay = startDate
                for (i in 0 .. 7) {
                    sleepDataList.add(SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))))
                }
                for (i in 0..6) {
                    dates.add(previousDay)
                    previousDay = Utils.getPreviousDateString(previousDay)
                }
                dates.add("") // Add one empty space for chart
                dates.reverse()

                val accessToken = Utils.readSharedSettingString("access_token", "")
                FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"

                val startDay = dates[1]

                val startDateFitbit = Utils.formatForFitbit(startDay)
                val endDateFitbit = Utils.formatForFitbit(dates.last())
                //Log.d("Dates reversed", dates.toString())

                val url = "/sleep/date/$startDateFitbit/$endDateFitbit.json"
                val (_, response, result) = url.httpGet().header(
                    "Authorization" to "Bearer $accessToken"
                ).responseString()

                val (sleepData, error) = result
                if (response.isSuccessful) {
                    val jsonObject = JSONTokener(sleepData).nextValue() as JSONObject
                    val jsonArray = jsonObject.optJSONArray("sleep")
                    if (jsonArray != null && jsonArray.length() > 0) {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val dateOfSleep = obj.getString("dateOfSleep")
                            val sleepLogDay = Utils.formatDateStringFromFitbit(dateOfSleep)
                            val summary = obj.getJSONObject("levels").getJSONObject("summary")
                            val deepSleep = summary.getJSONObject("deep").getInt("minutes")
                            val lightSleep = summary.getJSONObject("light").getInt("minutes")
                            val remSleep = summary.getJSONObject("rem").getInt("minutes")
                            val wakeSleep = summary.getJSONObject("wake").getInt("minutes")

                            val index = dates.indexOf(sleepLogDay)
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
                                val stringJson = jsonObject.toString(2)
                                val jsonMap: Map<String, Any> = Gson().fromJson(stringJson, object : TypeToken<HashMap<String, Any>>() {}.type)
                                saveSleepDataToFirebase(sleepLogDay, jsonMap)
                            }
                        }
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
            //Log.d("GetSleepDataFromThisWeek", "week's date: $startDateFitbit")
            FuelManager.instance.basePath = "https://api.fitbit.com/1.2/user/-"
            val url = "/sleep/date/$startDateFitbit.json"

            val (_, response, result) = url.httpGet().header(
                "Authorization" to
                        "Bearer $accessToken"
            ).responseString()
            val (sleepData, error) = result
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
