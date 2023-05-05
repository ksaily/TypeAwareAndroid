package com.example.testing.ui.menu

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.charts.CustomMarker
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.ui.viewmodel.ChartViewModel
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChartFragment : Fragment(R.layout.fragment_chart), SeekBar.OnSeekBarChangeListener,
    OnChartValueSelectedListener {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private val dateFragment = DateFragment()
    private val viewModel: ChartViewModel by activityViewModels()
    private val dateViewModel: DateViewModel by activityViewModels()
    private var barChart1: BarChart? = null
    private var barChart2: BarChart? = null
    private var chart: BarChart? = null

    private var seekBarX: SeekBar? = null
    private var seekBarY: SeekBar? = null
    private var tvX: TextView? = null
    private var tvY: TextView? = null

    // Chart variables:
    private val MAX_X_VALUE = 244
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Errors"
    private val GROUP_2_LABEL = "Words"
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.4f
    private val GROUP_SPACE = 0.1f
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Sessions", "Time window"
    )
    private val labels1= ArrayList<String>()
    private val labels2= ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        barChart1 = binding.barChart1
        barChart2 = binding.barChart2
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.dateContainer, dateFragment, "dateFragment")
            .addToBackStack("dateFragment").commit()

        val values1: ArrayList<BarEntry> = ArrayList()
        val values2: ArrayList<BarEntry> = ArrayList()

        statValues.clear()
        //
        for (i in 1 .. MAX_X_VALUE) {
            values1.add(
                BarEntry(
                    i.toFloat(),
                    (Math.random()).toFloat()
                )
            )
            labels1.add(i.toString())
        }

        for (i in 1.. MAX_X_VALUE) {
            values2.add(
                BarEntry(
                    i.toFloat(),
                    (Math.random() * 3).toFloat()
                )
            )
            labels2.add(i.toString())
        }
        //
        ////After preparing our data set, we need to display the data in our bar chart
        val v1: BarDataSet = BarDataSet(values1, "TestDataset1")
        // BarDataSet(firebaseViewModel.chartErrorValues.value, "Errors made")
        val v2: BarDataSet = BarDataSet(values2, "TestDataset2")
        val v3: BarDataSet = BarDataSet(values2, "TestDataset3")
        // BarDataSet(firebaseViewModel.chartSessions.value, "Sessions")



        val v4: BarDataSet = BarDataSet(values1, "TestDataset4")
        val data1: BarData = BarData()
        val data2: BarData = BarData()



        v1.setDrawValues(false)
        v1.color = R.color.light_purple
        v1.valueTextColor = R.color.white

        v3.setDrawValues(false)
        v3.color = R.color.white
        v1.valueTextColor = R.color.white

        //data1.addDataSet(v1)
        //data1.addDataSet(v2)
        data2.addDataSet(v3)
        //data2.addDataSet(v4)
        Log.d("Dataset1", data1.toString())
        dateViewModel.checkDate()
        //viewModel.getFromFirebaseToChart(dateViewModel.selectedDate.value.toString())

        viewModel.chartErrorValues.observe(viewLifecycleOwner) {
            Log.d("ChartView", "Errors found")
            val label1 = ArrayList<String>()
            val stats = viewModel.chartErrorValues.value
            val dats = mutableListOf<BarEntry>()
            if (stats != null) {
                for (i in stats) {
                    label1.add(i.toString())
            }} else {
                configureBarChart(barChart1!!, "Errors", label1)
                prepareChartData(barChart1!!, data1)
            }
            val v1: BarDataSet = BarDataSet(stats, "TestDataset1")
            v1.color = R.color.light_purple
            v1.valueTextColor = R.color.black
            val data = BarData()
            data.addDataSet(v1)
            Log.d("Dataset1", data1.toString())
            configureBarChart(barChart1!!, "Errors", label1)
            prepareChartData(barChart1!!, data)
        }

        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            Log.d("Dateviewmodel", "Date changed to: " +
                    dateViewModel.selectedDate.value)
            lifecycleScope.launch(Dispatchers.IO){
                try {
                    viewModel.getFromFirebaseToChart(dateViewModel.selectedDate.value.toString())
                } catch (e: Exception) {
                    Log.d("Error", "$e")
                }
            }
        }

        //configureBarChart(barChart1!!, "Errors per timewindow", labels1)
        configureBarChart(barChart2!!, "Sessions per timewindow", labels2)
        //prepareChartData(barChart1!!, data1)
        prepareChartData(barChart2!!, data2)



        //v2.addDataSet(dataSet2)
        //Set draw values to false (to avoid mess)

        //val entries = ArrayList<ILineDataSet>()
        //entries.add(v1)
        //entries.add(v2)

        //Make first dataset dashed
        //entries.get(0).formLineDashEffect
/**
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

        //Stacked Bar Chart
        barChart2 = binding.barChart2
        var xAxisValues = ArrayList<String>()

        xAxisValues.add("Jan")
        xAxisValues.add("Feb")
        xAxisValues.add("Mar")
        xAxisValues.add("Apr")
        xAxisValues.add("May")
        xAxisValues.add("June")
        xAxisValues.add("Jul")
        xAxisValues.add("Aug")
        xAxisValues.add("Sep")
        xAxisValues.add("Oct")
        xAxisValues.add("Nov")
        xAxisValues.add("Dec")

        var yValueGroup1 = ArrayList<BarEntry>()
        var yValueGroup2 = ArrayList<BarEntry>()


        yValueGroup1.add(BarEntry(1f, floatArrayOf(9.toFloat(), 3.toFloat())))
        yValueGroup2.add(BarEntry(1f, floatArrayOf(2.toFloat(), 7.toFloat())))
        yValueGroup1.add(BarEntry(2f, floatArrayOf(3.toFloat(), 3.toFloat())))
        yValueGroup2.add(BarEntry(2f, floatArrayOf(4.toFloat(), 15.toFloat())))

        yValueGroup1.add(BarEntry(3f, floatArrayOf(3.toFloat(), 3.toFloat())))
        yValueGroup2.add(BarEntry(3f, floatArrayOf(4.toFloat(), 15.toFloat())))

        yValueGroup1.add(BarEntry(4f, floatArrayOf(3.toFloat(), 3.toFloat())))
        yValueGroup2.add(BarEntry(4f, floatArrayOf(4.toFloat(), 15.toFloat())))


        yValueGroup1.add(BarEntry(5f, floatArrayOf(9.toFloat(), 3.toFloat())))
        yValueGroup2.add(BarEntry(5f, floatArrayOf(10.toFloat(), 6.toFloat())))

        yValueGroup1.add(BarEntry(6f, floatArrayOf(11.toFloat(), 1.toFloat())))
        yValueGroup2.add(BarEntry(6f, floatArrayOf(12.toFloat(), 2.toFloat())))


        yValueGroup1.add(BarEntry(7f, floatArrayOf(11.toFloat(), 7.toFloat())))
        yValueGroup2.add(BarEntry(7f, floatArrayOf(12.toFloat(), 12.toFloat())))


        yValueGroup1.add(BarEntry(8f, floatArrayOf(11.toFloat(), 9.toFloat())))
        yValueGroup2.add(BarEntry(8f, floatArrayOf(12.toFloat(), 8.toFloat())))


        yValueGroup1.add(BarEntry(9f, floatArrayOf(11.toFloat(), 13.toFloat())))
        yValueGroup2.add(BarEntry(9f, floatArrayOf(12.toFloat(), 12.toFloat())))

        yValueGroup1.add(BarEntry(10f, floatArrayOf(11.toFloat(), 2.toFloat())))
        yValueGroup2.add(BarEntry(10f, floatArrayOf(12.toFloat(), 7.toFloat())))

        yValueGroup1.add(BarEntry(11f, floatArrayOf(11.toFloat(), 6.toFloat())))

        yValueGroup2.add(BarEntry(11f, floatArrayOf(12.toFloat(), 5.toFloat())))

        yValueGroup1.add(BarEntry(12f, floatArrayOf(11.toFloat(), 2.toFloat())))
        yValueGroup2.add(BarEntry(12f, floatArrayOf(12.toFloat(), 3.toFloat())))

        var barDataSet1: BarDataSet
        var barDataSet2: BarDataSet


        barDataSet1 = BarDataSet(yValueGroup1, "")
        barDataSet1.setColors(Color.BLUE, Color.RED)

        barDataSet1.setDrawIcons(false)
        barDataSet1.setDrawValues(false)

        barDataSet2 = BarDataSet(yValueGroup2, "")
        barDataSet2.setColors(Color.YELLOW, Color.RED)
        barDataSet2.setDrawIcons(false)
        barDataSet2.setDrawValues(false)


        // Pass Both Bar Data Set's in Bar Data.
        var barData = BarData(barDataSet1, barDataSet2)

        chart!!.description.isEnabled = false
        chart!!.description.textSize = 0f
        barData.setValueFormatter(LargeValueFormatter())
        chart!!.data = barData
        chart!!.barData.barWidth = BAR_WIDTH
        chart!!.xAxis.axisMinimum = 0f
        chart!!.xAxis.axisMaximum = 12f
        chart!!.groupBars(0f, GROUP_SPACE, BAR_SPACE)
//   barChartView.setFitBars(true)
        chart!!.data.isHighlightEnabled = false
        chart!!.invalidate()

        // set bar label
        var legend = chart!!.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL)
        legend.setDrawInside(false)

        var legenedEntries = arrayListOf<LegendEntry>()

        legenedEntries.add(LegendEntry("2016", Legend.LegendForm.SQUARE, 8f, 8f, null, Color.RED))
        legenedEntries.add(LegendEntry("2017", Legend.LegendForm.SQUARE, 8f, 8f, null, Color.YELLOW))

        legend.setCustom(legenedEntries)

        legend.setYOffset(2f)
        legend.setXOffset(2f)
        legend.setYEntrySpace(0f)
        legend.setTextSize(5f)


        //Populate x-axis
        val xAxis = chart!!.getXAxis()
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 9f

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)

        xAxis.labelCount = 12
        xAxis.mAxisMaximum = 12f
        xAxis.setCenterAxisLabels(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.spaceMin = 4f
        xAxis.spaceMax = 4f

        //Y-axis
        val leftAxis = chart!!.axisLeft
        leftAxis.valueFormatter = LargeValueFormatter()
        leftAxis.setDrawGridLines(false)
        leftAxis.spaceTop = 1f
        leftAxis.axisMinimum = 0f

        chart!!.data = barData
        chart!!.setVisibleXRange(1f, 12f)**/
    }

    override fun onResume() {
        super.onResume()
        //getFromFirebase("2023-01-05")
        //binding.keyspeedData.text = avgSpeed.toString()
        //Sets the progressbar correctly and returns
        //binding.ProgressTextView.text = showPercentage(avgErrors, binding.progressCircular).toString()

    }
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        //tvX!!.text = seekBarX!!.progress.toString()
        //tvY!!.text = seekBarY!!.progress.toString()
        /**
        val values = ArrayList<BarEntry>()
        for (i in 0 until 10) {
            val mul = (i + 1).toFloat()
            val val1 = (Math.random() * mul).toFloat() + mul / 3
            val val2 = (Math.random() * mul).toFloat() + mul / 3
            val val3 = (Math.random() * mul).toFloat() + mul / 3
            values.add(BarEntry(
                i.toFloat(),
                floatArrayOf(val1, val2, val3)))
        }
        val set1: BarDataSet
        if (chart!!.data != null &&
            chart!!.data.dataSetCount > 0
        ) {
            set1 = chart!!.data.getDataSetByIndex(0) as BarDataSet
            set1.values = values
            chart!!.data.notifyDataChanged()
            chart!!.notifyDataSetChanged()
        } else {
            set1 = BarDataSet(values, "Statistics Vienna 2014")
            set1.setDrawIcons(false)
            //set1.setColors(*colors)
            set1.stackLabels = arrayOf("Births", "Divorces", "Marriages")
            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(set1)
            val data = BarData(dataSets)
            //data.setValueFormatter(MyValueFormatter())
            data.setValueTextColor(Color.WHITE)
            chart!!.data = data
        }
        chart!!.setFitBars(true)
        chart!!.invalidate()**/
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onValueSelected(e: Entry, h: Highlight) {
        val entry = e as BarEntry
        if (entry.yVals != null) Log.i("VAL SELECTED",
            "Value: " + entry.yVals[h.stackIndex]) else Log.i("VAL SELECTED", "Value: " + entry.y)
    }

    private fun prepareChartData(chart: BarChart, data: BarData) {
        if (chart == barChart1) {
            barChart1!!.data = data
            barChart1!!.data.setValueFormatter(IndexAxisValueFormatter(labels1))
            barChart1!!.barData.barWidth = BAR_WIDTH
            val groupSpace = 1f - (BAR_SPACE + BAR_WIDTH)
            //barChart1!!.groupBars(0f, groupSpace, BAR_SPACE)
            //barChart1!!.data.setValueTextColor(R.color.white)
            barChart1!!.invalidate()
        } else {
            barChart2!!.data = data
            barChart2!!.barData.barWidth = BAR_WIDTH
            val groupSpace = 1f - (BAR_SPACE + BAR_WIDTH)
            //barChart2!!.groupBars(0f, groupSpace, BAR_SPACE)
            //barChart2!!.data.setValueTextColor(R.color.white)
            barChart2!!.invalidate()
        }

    }

    private fun configureBarChart(chart: BarChart, description: String, xAxisValues: ArrayList<String>) {
        //chart!!.setBackgroundColor(R.color.white)
        //chart!!.setDrawBarShadow(false)
        chart!!.setDrawGridBackground(false)

        chart!!.description.isEnabled = true
        chart!!.description.text = description
        chart!!.description.textColor = R.color.white
        val xAxis = chart!!.xAxis
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setAxisMinimum(0 + 0.5f); //to center the bars inside the vertical grid lines we need + 0.5 step
        xAxis.setAxisMaximum(244f + 0.5f); //to center the bars inside the vertical grid lines we need + 0.5 step
        //xAxis.setLabelCount(6, false); //show only 5 labels (5 vertical grid lines)
        xAxis.setXOffset(0f); //labels x offset in dps
        xAxis.setYOffset(0f); //labels y offset in dps
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
        chart!!.xAxis.isEnabled = true
        Log.d("Chart labels", labels1.toString())
        val leftAxis = chart!!.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.spaceTop = 35f
        leftAxis.axisMinimum = 0f
        chart!!.axisRight.isEnabled = false
        chart!!.xAxis.axisMinimum = 1f
        chart!!.xAxis.axisMaximum = MAX_X_VALUE.toFloat()
        //xAxis.setCenterAxisLabels(true)
        val j = 0
        //To remove right side y axis from chart:
        chart!!.axisRight?.isEnabled = false
        chart!!.xAxis.axisMaximum = 5f+0.1f

        //To enable zooming the chart
        chart!!.setTouchEnabled(true)
        chart!!.setPinchZoom(true)

        //When dataset fails write this on the screen
        chart!!.setNoDataText("No data yet!")
        chart!!.setNoDataTextColor(R.color.black)
        //chart!!.xAxis.textColor = R.color.white
        //leftAxis.textColor = R.color.white
        //chart!!.data.setValueTextColor(R.color.white)

        //Add animation to show while the dataset is loading
        chart!!.animateX(1800, Easing.EaseInExpo)

        //If you want to show values on the linechart, create custom market for that
        // Remember to create a layout for this
        val markerView = CustomMarker(Graph.appContext, R.layout.marker_view)
        chart!!.marker = markerView
    }


    override fun onNothingSelected() {}


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