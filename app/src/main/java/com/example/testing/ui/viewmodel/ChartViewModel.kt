package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.collection.arraySetOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testing.fitbit.FitbitApiService.Companion.getRefreshToken
import com.example.testing.data.KeyboardChart
import com.example.testing.data.SleepData
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
    //private val sleepDataList = ArrayList<SleepDataForChart>()
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
        for (i in 0..144) {
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

    private fun createSleepDataList(): ArrayList<SleepDataForChart> {
        return arrayListOf(
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f))),
            SleepDataForChart("", BarEntry(0f, floatArrayOf(0f,0f,0f,0f)))
        )

    }

    fun getSleepDataFromThisWeek(startDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sleepDataList = createSleepDataList()
                val dates = arrayListOf<String>()
                var previousDay = startDate
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
                                val stringJson = obj.toString(2)
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

}

data class SleepDataForChart(
    val date: String,
    val entry: BarEntry
)
