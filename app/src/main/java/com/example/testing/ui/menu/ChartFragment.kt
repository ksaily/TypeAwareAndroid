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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.charts.CustomMarker
import com.example.testing.charts.StackedBarChartFragment
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.ui.viewmodel.ChartViewModel
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.ui.viewmodel.SleepDataForChart
import com.example.testing.utils.Utils
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
    private var stackedBarChart: BarChart? = null
    private var chart: BarChart? = null

    private var seekBarX: SeekBar? = null
    private var seekBarY: SeekBar? = null
    private var tvX: TextView? = null
    private var tvY: TextView? = null

    // Chart variables:
    private val MAX_X_VALUE = 144
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Errors"
    private val GROUP_2_LABEL = "Words"
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.6f
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
        stackedBarChart = binding.StackedBarChart
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!dateFragment.isAdded) {
            val transaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.dateContainer, dateFragment, "dateFragment")
                .addToBackStack("dateFragment").commit()
        }

        val values1: ArrayList<BarEntry> = ArrayList()
        val values2: ArrayList<BarEntry> = ArrayList()
        val typefaceBolded = ResourcesCompat.getFont(Graph.appContext, R.font.roboto_black)
        val typefaceNormal = ResourcesCompat.getFont(Graph.appContext, R.font.roboto_bold)

        statValues.clear()
        viewModel.chartSelected = 0 //Initiate selected charts to error
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
        v1.valueTextColor = R.color.dark_purple

        v3.setDrawValues(false)
        v3.color = R.color.light_purple
        v3.valueTextColor = R.color.dark_purple

        //data1.addDataSet(v1)
        //data1.addDataSet(v2)
        data2.addDataSet(v3)
        //data2.addDataSet(v4)
        Log.d("Dataset1", data1.toString())
        dateViewModel.checkDate()
        lifecycleScope.launch {
            viewModel.getFromFirebaseToChart(dateViewModel.selectedDate.value.toString())
        }
        if (Utils.getSharedPrefs().contains(getString(R.string.sharedpref_access))) {
            viewModel.getSleepDataFromThisWeek(dateViewModel.selectedDate.value.toString())
        }

        binding.switchToWritingSpeedBtn.setOnClickListener {
            if (viewModel.chartSelected == 0) {
                Log.d("ChartFragment", "Switching chart to WPM")
                binding.switchToWritingSpeedBtn.setTextAppearance(R.style.switchChartChosen)
                binding.switchToErrorsBtn.setTextAppearance(R.style.switchChartNotChosen)
                binding.switchChartTitle.text = "Words per minute"
                viewModel.chartSelected = 1
                val stats = viewModel.chartSpeedValues.value
                if (stats != null) {
                    updateChart(
                        viewModel.chartSpeedValues.value!!, "Words per minute",
                        "WPM", barChart1!!
                    )
                }
            }
        }

        binding.switchToErrorsBtn.setOnClickListener {
            if (viewModel.chartSelected == 1) {
                Log.d("ChartFragment", "Switching chart to error")
                binding.switchToErrorsBtn.setTextAppearance(R.style.switchChartChosen)
                binding.switchToWritingSpeedBtn.setTextAppearance(R.style.switchChartNotChosen)
                binding.switchChartTitle.text = "Errors"
                viewModel.chartSelected = 0
                val stats = viewModel.chartErrorValues.value
                if (stats != null) {
                    updateChart(
                        viewModel.chartErrorValues.value!!, "Errors made",
                        "Errors", barChart1!!
                    )
                }
            }
        }



        viewModel.chartErrorValues.observe(viewLifecycleOwner) {
            Log.d("ChartView", "Errors found")
            if (viewModel.chartSelected == 0) {
                val stats = viewModel.chartErrorValues.value
                if (stats != null) {
                    updateChart(stats, "Errors made", "Errors", barChart1!!)
                    barChart1!!.notifyDataSetChanged()
                }
            }
        }

        viewModel.chartSpeedValues.observe(viewLifecycleOwner) {
            Log.d("ChartView", "Speedvalues found")
            if (viewModel.chartSelected == 1) {
                val stats = viewModel.chartSpeedValues.value
                if (stats != null) {
                    updateChart(stats, "Words per minute", "WPM", barChart1!!)
                    barChart1!!.notifyDataSetChanged()
                }
            }
        }

        viewModel.chartSessions.observe(viewLifecycleOwner) {
            Log.d("ChartView", "Session values found")
            val stats = viewModel.chartSessions.value
            if (stats != null) {
                    updateChart(stats, "Sessions", "Amount of sessions", barChart2!!)
                    barChart2!!.notifyDataSetChanged()
            }
        }


        viewModel.sleepDataValues.observe(viewLifecycleOwner) {
            Log.d("ChartView", "Sleep values found")
            val stats = viewModel.sleepDataValues.value
            if (stats != null) {
                updateStackedChart(stats, "Weekly sleep quality", "Sleep quality")
                stackedBarChart!!.notifyDataSetChanged()
            }
        }

        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            Log.d("Dateviewmodel", "Date changed to: " +
                    dateViewModel.selectedDate.value)
            lifecycleScope.launch(Dispatchers.IO){
                try {
                    viewModel.getFromFirebaseToChart(dateViewModel.selectedDate.value.toString())
                    viewModel.getSleepDataFromThisWeek(dateViewModel.selectedDate.value.toString())
                } catch (e: Exception) {
                    Log.d("Error", "$e")
                }
            }
            //barChart1!!.notifyDataSetChanged()
            //barChart2!!.notifyDataSetChanged()
        }

        //configureBarChart(barChart1!!, "Errors per timewindow", labels1)
        //configureBarChart(barChart2!!, "Sessions per timewindow", labels2)
        //prepareChartData(barChart1!!, data1)
        //prepareChartData(barChart2!!, data2)
    }

    private fun updateChart(stats: List<BarEntry>, label: String,
                            description: String, chart: BarChart) {
        val label1 = ArrayList<String>()
        for (i in stats) {
            label1.add(i.x.toInt().toString())
        }
        val v1: BarDataSet = BarDataSet(stats, label)
        v1.setDrawValues(false)
        val data = BarData()
        data.addDataSet(v1)
        configureBarChart(chart!!, description, label1)
        prepareChartData(chart!!, data)
    }


    private fun updateStackedChart(stats: List<SleepDataForChart>, label: String,
                            description: String) {
        val labels = ArrayList<String>()
        val datasetList = ArrayList<BarEntry>()
        for (i in stats) {
            labels.add(i.date)
            datasetList.add(i.entry)
        }
        val v1: BarDataSet = BarDataSet(datasetList, label)
        v1.setDrawValues(false)
        v1.stackLabels = arrayOf("deep", "light", "rem", "wake")
        v1.colors = listOf(R.color.dark_purple, R.color.light_purple,
            R.color.muted_light_purple, R.color.teal_700)
        val data = BarData()
        data.addDataSet(v1)
        configureBarChart(stackedBarChart!!, description, labels)
        prepareChartData(stackedBarChart!!, data)
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
        chart.data = data
        chart.barData.barWidth = BAR_WIDTH
        chart.invalidate()
        if (chart == stackedBarChart) {
            chart.setVisibleXRangeMaximum(7f)
        } else {
            chart.setVisibleXRangeMaximum(16f)
        }
    }

    private fun configureBarChart(chart: BarChart, description: String, xAxisValues: ArrayList<String>) {
        //chart!!.setBackgroundColor(R.color.white)
        //chart!!.setDrawBarShadow(false)
        chart!!.setDrawGridBackground(false)

        chart!!.description.isEnabled = false
        //chart!!.description.text = description
        //chart!!.description.textColor = R.color.dark_purple



        val xAxis = chart!!.xAxis
        //xAxis.labelCount = 12
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM


        xAxis.axisMinimum = 0 + 0.5f; //to center the bars inside the vertical grid lines we need + 0.5 step
        xAxis.axisMaximum = 144f + 0.5f; //to center the bars inside the vertical grid lines we need + 0.5 step
        //xAxis.setLabelCount(12, false); //show only 5 labels (5 vertical grid lines)
        xAxis.xOffset = 0f; //labels x offset in dps
        xAxis.yOffset = 0f; //labels y offset in dps
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
        chart!!.xAxis.isEnabled = true
        Log.d("Chart labels", labels1.toString())
        val leftAxis = chart!!.axisLeft
        leftAxis.setDrawGridLines(false)
        //leftAxis.spaceTop = 35f
        leftAxis.axisMinimum = 0f
        chart!!.axisRight.isEnabled = false
        // Limit the number of visible X-axis labels to 20

// Enable horizontal scrolling and scaling
        chart.isDragEnabled = true
        chart.isScaleXEnabled = true
        //chart!!.xAxis.axisMinimum = 1f
        //chart!!.xAxis.axisMaximum = MAX_X_VALUE.toFloat()
        //xAxis.setCenterAxisLabels(true)
        val j = 0
        //To remove right side y axis from chart:
        chart!!.axisRight?.isEnabled = false

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