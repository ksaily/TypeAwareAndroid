package com.example.testing.ui.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.testing.R
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.utils.Utils.Companion.getFromFirebase
import java.util.*

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
    }
}