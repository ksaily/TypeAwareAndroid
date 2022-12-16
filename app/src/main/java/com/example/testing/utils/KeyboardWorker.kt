package com.example.testing.utils

import android.content.Context
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
                dataList.remove(instance)
            }
            //dataList = newList as MutableList<KeyboardEvents>
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun saveToFirebase(timeslot: Int, event: KeyboardEvents) {
        val database = Firebase.database("https://health-app-9c151-default-rtdb.europe-west1.firebasedatabase.app")
        val myRef = database.getReference("KeyboardEvents")
        // Save data under the current timeslot with an unique id for each
        val dateString = "Date:" + event.date
        val timeSlotString = "Timeslot:$timeslot"
        myRef.child("events").child(dateString).child(timeSlotString).child(event.id.toString()).setValue(event)
    }
}