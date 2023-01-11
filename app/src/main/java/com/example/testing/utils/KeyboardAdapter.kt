package com.example.testing.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.testing.ChartActivity
import com.example.testing.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.collections.ArrayList

class KeyboardAdapter : RecyclerView.Adapter<KeyboardAdapter.ViewHolder>() {
    private val data = arrayListOf<KeyboardEvents>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var speedItem : TextView
        lateinit var accuracyItem : TextView
        lateinit var accuracyCircle : ProgressBar

        init {
            speedItem = itemView.findViewById(R.id.speedData)
            accuracyItem = itemView.findViewById(R.id.ProgressTextView)
            accuracyCircle = itemView.findViewById(R.id.progress_circular)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fragment_chart, viewGroup, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        getFromFirebase("2023-01-10")
        viewHolder.speedItem.text = avgSpeed.toString()
        //Sets the progressbar correctly and returns
        viewHolder.accuracyItem.text = ChartActivity.showPercentage(avgErrors, viewHolder.accuracyCircle).toString()
    }

    override fun getItemCount(): Int {
        return data.size
    }

companion object {
    var totalAvgSpeed: ArrayList<Double> = arrayListOf()
    var totalAvgErrors: ArrayList<Int> = arrayListOf()
    var avgSpeed: Double = 0.0
    var avgErrors: Double = 0.0
    /**
     * Count average for one instance in firebase database,
     * which is a list of typing speeds and
     * return the average speed for that instance
     */
    fun countAvgSpeed(speed: List<Double>): Double {
        var total = 0.0
        for (i in speed) {
            total += i
        }
        return total / speed.size
    }

    fun countAvgErrors(errors: ArrayList<Int>): Double {
        var total = 0.0
        for (i in errors) {
            total += i
        }
        return total / totalAvgErrors.size
    }

    fun getFromFirebase(date: String) {
        val rootRef = FirebaseDatabase.getInstance().reference
        rootRef.child(date)
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val content = snapshot.children
                for (i in content) {
                    var list = i.children
                    list.forEach {
                        var speeds = it.child("typingSpeed").value
                        var avgForOne = countAvgSpeed(speeds as List<Double>)
                        totalAvgErrors.add(it.child("errorAmount").value as Int)
                        //Add the average for one instance to a new list
                        totalAvgSpeed.add(avgForOne)
                    }
                    Log.d("Firebase", list.toString())
                    avgErrors = countAvgErrors(totalAvgErrors)
                    avgSpeed = countAvgSpeed(totalAvgSpeed)
                    var data = KeyboardStats(date, i.key, avgErrors, avgSpeed)
                    println(data)
                }
                Log.d("Firebase","Data fetched from firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Firebase", error.message)
            }
        }
    }
    fun showPercentage(errorRate: Double, progressBar: ProgressBar, ): Double {
        var successRate = (1.0 - errorRate) * 100
        progressBar.progress = successRate.toInt()
        return successRate
    }}
}