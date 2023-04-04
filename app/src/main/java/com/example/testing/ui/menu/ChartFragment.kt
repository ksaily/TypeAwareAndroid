package com.example.testing.ui.menu

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.charts.CustomMarker
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.utils.Utils.Companion.getFromFirebase
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChartFragment : Fragment(R.layout.fragment_chart) {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val dateFragment = DateFragment()
    private var lineChart: LineChart? = null

    // Chart variables:
    private val MAX_X_VALUE = 20
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Sessions"
    private val GROUP_2_LABEL = "Time window"
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.8f
    private var chart: BarChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Sessions", "Time window"
    )

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
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.dateContainer, dateFragment, "dateFragment")
            .addToBackStack("dateFragment").commit()
        lineChart = binding.sleepDataChart
        var values1: ArrayList<Entry> = ArrayList()
        var values2: ArrayList<Entry> = ArrayList()
        statValues.clear()
        //
        for (i in 0 until MAX_X_VALUE) {
            values1.add(
                Entry(
                    i.toFloat(),
                    (Math.random() * 80).toFloat()
                )
            )
        }

        for (i in 0 until MAX_X_VALUE) {
            values2.add(
                BarEntry(
                    i.toFloat(),
                    (Math.random() * 80).toFloat()
                )
            )
        }
        //
        ////After preparing our data set, we need to display the data in our bar chart
        val v1: LineDataSet = LineDataSet(values1, "Test")
        val v2: LineDataSet = LineDataSet(values2, "Test")
        //val v1: LineData = LineData()
        //val v2: LineData = LineData()
        //v1.addDataSet(dataSet1)
        //v2.addDataSet(dataSet2)
        //Set draw values to false (to avoid mess)
        v1.setDrawValues(false)
        v1.setDrawFilled(true)
        v1.lineWidth = 3f
        v1.fillColor = R.color.light_purple

        v2.setDrawValues(false)
        v2.setDrawFilled(false)
        v2.lineWidth = 3f
        v2.fillColor = R.color.white

        val entries = ArrayList<ILineDataSet>()
        entries.add(v1)
        entries.add(v2)

        //Make first dataset dashed
        entries.get(0).formLineDashEffect

        //Assign our list to LineDataSet and label it
        //val v1 = LineDataSet(entries, "My type")

        //Set label rotation angle to x axis
        lineChart!!.xAxis.labelRotationAngle = 0f

        //Assign dataset to line chart
        lineChart!!.data = LineData(entries)
        //lineChart!!.data = LineData(v2)

        val j = 0
        //To remove right side y axis from chart:
        lineChart!!.axisRight?.isEnabled = false
        lineChart!!.xAxis.axisMaximum = 5f+0.1f

        //To enable zooming the chart
        lineChart!!.setTouchEnabled(true)
        lineChart!!.setPinchZoom(true)

        //When dataset fails write this on the screen
        lineChart!!.description.text = "Days"
        lineChart!!.setNoDataText("No forex yet!")
        lineChart!!.setNoDataTextColor(R.color.white)
        lineChart!!.xAxis.textColor = R.color.white
        lineChart!!.data.setValueTextColor(R.color.white)

        //Add animation to show while the dataset is loading
        lineChart!!.animateX(1800, Easing.EaseInExpo)

        //If you want to show values on the linechart, create custom market for that
        // Remember to create a layout for this
        val markerView = CustomMarker(Graph.appContext, R.layout.marker_view)
        lineChart!!.marker = markerView
        lineChart!!.invalidate()
        }

    override fun onResume() {
        super.onResume()
        //getFromFirebase("2023-01-05")
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