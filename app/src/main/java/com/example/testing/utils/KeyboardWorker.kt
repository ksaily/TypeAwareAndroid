package com.example.testing.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.testing.utils.KeyboardHelper.Companion.dataList
import com.example.testing.utils.KeyboardHelper.Companion.previousTimeSlot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class KeyboardWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    /** Save events with same dailytimewindow to Firebase,
     * and remove them from the list of KeyboardEvents */
    override fun doWork(): Result {
        return try {
            val keyboardTimeslot =
                inputData.getInt("TIMESLOT", 0)
            val newList = dataList.filterNot {
                !it.dailyTimeWindow.equals(keyboardTimeslot)
            }
            Log.d("FirebaseDebug", "New list: $newList")
            for (instance in newList) {
                saveToFirebase(keyboardTimeslot, instance)
                if (dataList.isNotEmpty()) {
                    dataList.remove(instance)
                }
            }
            //dataList = newList as MutableList<KeyboardEvents>
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun saveToFirebase(timeslot: Int, event: KeyboardEvents) {
        val myRef = Firebase.database.getReference("Data")
        // Save data under the current timeslot with an unique id for each
        val dateString = event.date
        val participantId = Utils.readSharedSettingString(
            applicationContext,
            "p_id",
            "")
        myRef.child(participantId.toString())
            .child(dateString).child("keyboardEvents").child(timeslot.toString())
                .child(event.id.toString()).setValue(event)
    }
}