package com.example.testing.ui.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.testing.ChartActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.utils.KeyboardAdapter
import com.example.testing.utils.KeyboardAdapter.Companion.getFromFirebase
import com.example.testing.utils.KeyboardStats
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class ChartFragment : Fragment(R.layout.fragment_chart) {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val calendar: Calendar = Calendar.getInstance()
    private val year = calendar.get(Calendar.YEAR)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //layoutManager = LinearLayoutManager(activity)
        //adapter = KeyboardAdapter()
        //Set date to today

        getFromFirebase("2023-01-05")
        Log.d("Firebaseinfo", "OnViewCreated")
        binding.speedData.text = avgSpeed.toString()
        //Sets the progressbar correctly and returns
        binding.ProgressTextView.text = showPercentage(avgErrors, binding.progressCircular).toString()
        }


    companion object {
        var totalAvgSpeed: ArrayList<Double> = arrayListOf()
        var totalAvgErrors: ArrayList<Int> = arrayListOf()
        var avgSpeed: Double = 0.0
        var avgErrors: Double = 0.0
        var currentDate: String = ""
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

        fun getCurrentDate() {
            val time = Calendar.getInstance().time
            val formatter = SimpleDateFormat("yyyy-MM-dd")
            currentDate = formatter.format(time)
        }

        fun getFromFirebase(date: String) {
            getCurrentDate()
            val rootRef = FirebaseDatabase.getInstance().reference
            val ref = rootRef.child(date)
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val content = snapshot.children
                    content.forEach { dataSnapshot ->
                        var list = dataSnapshot.children
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
                        var data = KeyboardStats(date, dataSnapshot.key, avgErrors, avgSpeed)
                        println(data)
                    }
                    Log.d("Firebase", "Data fetched from firebase")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Firebase", error.message)
                }
            }
            if (date != currentDate) {
                ref.addListenerForSingleValueEvent(valueEventListener)
            } else {
                ref.addValueEventListener(valueEventListener)
            }
        }

        fun showPercentage(errorRate: Double, progressBar: ProgressBar, ): Double {
            var successRate = (1.0 - errorRate) * 100
            progressBar.progress = successRate.toInt()
            return successRate
        }
    }
}