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
import com.example.testing.utils.KeyboardEvents
import com.example.testing.utils.KeyboardStats
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChartFragment : Fragment(R.layout.fragment_chart) {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

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
        }

    override fun onResume() {
        super.onResume()
        getFromFirebase("2023-01-05")
        Log.d("Firebaseinfo", "onResume")
        //binding.keyspeedData.text = avgSpeed.toString()
        //Sets the progressbar correctly and returns
        //binding.ProgressTextView.text = showPercentage(avgErrors, binding.progressCircular).toString()

    }


    companion object {
        var keyboardList: ArrayList<KeyboardStats> = arrayListOf()
        var errorsList: MutableList<Long> = mutableListOf()
        var totalErrList: MutableList<Long> = mutableListOf()
        var totalSpeedsList: MutableList<MutableList<Double>> = mutableListOf()
        var totalAvgErrors: ArrayList<Long> = arrayListOf()
        var timeWindow: String = ""
        var totalErr: Double = 0.0
        var totalSpeed: Double = 0.0
        var speedsList: MutableList<Double> = mutableListOf()
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        var currentDate: String = getCurrentDateString()
        /**
         * Count average for one instance in firebase database,
         * which is a list of typing speeds and
         * return the average speed for that instance
         */

        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }


        fun countAvgSpeed(speed: MutableList<Double>): Double {
            var total = 0.0
            for (i in speed) {
                total += i
            }
            return total / speed.size
        }

        fun countAvgErrors(errors: MutableList<Long>): Double {
            var total = 0.0
            for (i in errors) {
                total += i
            }
            return total / errors.size
        }

        fun getCurrentDateString(): String {
            var time = Calendar.getInstance().time
            return formatter.format(time)
        }


        fun getPreviousDateString(inputDate: String): String {
            val cal = Calendar.getInstance()
            var date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, -1)
            var previousDate = formatter.format(cal.time)
            Log.d("Dates", "Selected date: $inputDate")
            Log.d("Dates", "Previous date: $date")
            return previousDate
        }

        fun getNextDateString(inputDate: String): String {
            val cal = Calendar.getInstance()
            var date = formatter.parse(inputDate) as Date
            cal.time = date
            cal.add(Calendar.DATE, +1)
            var inputDate = formatter.format(cal.time)
            Log.d("Dates", "Selected date: $inputDate")
            Log.d("Dates", "Previous date: $date")
            return inputDate

        }

        fun getFromFirebase(date: String) {
            val rootRef = FirebaseDatabase.getInstance().reference
            val ref = rootRef.child(date)
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val children = snapshot.children
                        if (children != null) {
                        children.forEach { dataSnapshot ->
                                var child = dataSnapshot.children
                                child.forEach {
                                    var speeds = it.child("typingSpeed").value
                                    var avgForOne = countAvgSpeed(speeds as MutableList<Double>)
                                    errorsList.add(it.child("errorAmount").value as Long)
                                    //Add the average for one instance to a new list
                                    speedsList.add(avgForOne)
                                }
                                totalErrList = (totalErrList + errorsList).toMutableList()
                                Log.d("Firebase", child.toString())
                                totalSpeedsList.add(speedsList.toMutableList())
                                timeWindow = dataSnapshot.key.toString()
                                //avgSpeed = countAvgSpeed(totalAvgSpeed)
                                //var data = KeyboardStats(date, dataSnapshot.key, avgErrors, avgSpeed)
                                //println(data)
                            }
                            totalErr = countAvgErrors(totalErrList)
                            var total: MutableList<Double> = mutableListOf()
                            for (i in totalSpeedsList) {
                                total.add(countAvgSpeed(i))
                            }
                            totalSpeed = countAvgSpeed(total)
                            var data = KeyboardStats(currentDate, timeWindow, totalErr, totalSpeed)
                            Log.d("Firebase", "Data fetched from firebase")
                            println(data)
                            keyboardList.add(data)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Firebase", error.message)
                }
            }
            if (date != currentDate) {
                rootRef.addListenerForSingleValueEvent(valueEventListener)
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