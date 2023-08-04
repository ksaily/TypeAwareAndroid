package com.example.testing.ui.menu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.ui.viewmodel.ChartViewModel
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.SleepDataForChart
import com.example.testing.utils.Utils
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

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

    // Chart variables:
    private val BAR_WIDTH = 0.6f
    private val statValues: ArrayList<Float> = ArrayList()
    private var lightPurple: Int = 0
    private var darkPurple: Int = 0
    private var teal: Int = 0
    private var mutedPurple: Int = 0
    private val xAxisTimeOfDayLabel: ArrayList<String> = arrayListOf()

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
        lightPurple = getColor(resources, R.color.light_purple, null)
        teal = getColor(resources, R.color.teal_200, null)
        darkPurple = getColor(resources, R.color.dark_purple, null)
        mutedPurple = getColor(resources, R.color.muted_light_purple, null)

        statValues.clear()
        viewModel.chartSelected = 0 //Initiate selected charts to error

        dateViewModel.checkDate()
        lifecycleScope.launch {
            viewModel.getFromFirebaseToChart(dateViewModel.selectedDate.value.toString())
        }

        if (isLoggedInFitbit()) {
            viewModel.getSleepDataFromThisWeek(dateViewModel.selectedDate.value.toString())
        }

        binding.switchToWritingSpeedBtn.setOnClickListener {
            if (viewModel.chartSelected == 0) {
                binding.switchToWritingSpeedBtn.setTextAppearance(R.style.switchChartChosen)
                binding.switchToErrorsBtn.setTextAppearance(R.style.switchChartNotChosen)
                binding.switchToWritingSpeedBtn.setBackgroundResource(R.drawable.border_selected)
                binding.switchToErrorsBtn.setBackgroundResource(R.drawable.border_unselected)
                binding.switchChartTitle.text = "Words per minute"
                viewModel.chartSelected = 1
                val stats = viewModel.chartSpeedValues.value
                if (stats != null) {
                    updateChart(
                        viewModel.chartSpeedValues.value!!, "WPM",
                        "Time of day", barChart1!! )
                }
            }
        }

        binding.switchToErrorsBtn.setOnClickListener {
            if (viewModel.chartSelected == 1) {
                binding.switchToErrorsBtn.setTextAppearance(R.style.switchChartChosen)
                binding.switchToWritingSpeedBtn.setTextAppearance(R.style.switchChartNotChosen)
                binding.switchChartTitle.text = "Error rates"
                binding.switchToWritingSpeedBtn.setBackgroundResource(R.drawable.border_unselected)
                binding.switchToErrorsBtn.setBackgroundResource(R.drawable.border_selected)
                viewModel.chartSelected = 0
                val stats = viewModel.chartErrorValues.value
                if (stats != null) {
                    updateChart(
                        viewModel.chartErrorValues.value!!, "Error rate",
                        "Time of day", barChart1!! )
                }
            }
        }



        viewModel.chartErrorValues.observe(viewLifecycleOwner) {
            if (viewModel.chartSelected == 0) {
                val stats = viewModel.chartErrorValues.value
                if (stats != null) {
                    updateChart(stats, "Error rate", "Time of day", barChart1!!)
                    barChart1!!.notifyDataSetChanged()
                }
            }
        }

        viewModel.chartSpeedValues.observe(viewLifecycleOwner) {
            if (viewModel.chartSelected == 1) {
                val stats = viewModel.chartSpeedValues.value
                if (stats != null) {
                    updateChart(stats, "WPM", "Time of day", barChart1!!)
                    barChart1!!.notifyDataSetChanged()
                }
            }
        }

        viewModel.chartSessions.observe(viewLifecycleOwner) {
            val stats = viewModel.chartSessions.value
            if (stats != null) {
                updateChart(stats, "Sessions", "Time of day", barChart2!!)
                barChart2!!.notifyDataSetChanged()
            }
        }


        viewModel.sleepDataValues.observe(viewLifecycleOwner) {
            val stats = viewModel.sleepDataValues.value
            if (stats != null) {
                updateStackedChart(stats, "Sleep stages in minutes", "Date")
                stackedBarChart!!.notifyDataSetChanged()
            }
        }

        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            clearAllChartData()
            lifecycleScope.launch(Dispatchers.IO){
                try {
                    viewModel.getFirebaseData(dateViewModel.selectedDate.value.toString())
                    viewModel.getSleepDataFromThisWeek(dateViewModel.selectedDate.value.toString())
                } catch (e: Exception) {
                    Log.d("Error", "$e")
                }
            }
        }
        xAxisTimeOfDayLabel.clear()
        for (i in 0..144) {
            when (i) {
                0 -> xAxisTimeOfDayLabel.add("12AM")
                6 -> xAxisTimeOfDayLabel.add("1AM")
                12 -> xAxisTimeOfDayLabel.add("2AM")
                18 -> xAxisTimeOfDayLabel.add("3AM")
                24 -> xAxisTimeOfDayLabel.add("4AM")
                30 -> xAxisTimeOfDayLabel.add("5AM")
                36 -> xAxisTimeOfDayLabel.add("6AM")
                42 -> xAxisTimeOfDayLabel.add("7AM")
                48 -> xAxisTimeOfDayLabel.add("8AM")
                54 -> xAxisTimeOfDayLabel.add("9AM")
                60 -> xAxisTimeOfDayLabel.add("10AM")
                66 -> xAxisTimeOfDayLabel.add("11AM")
                72 -> xAxisTimeOfDayLabel.add("12PM")
                78 -> xAxisTimeOfDayLabel.add("1PM")
                84 -> xAxisTimeOfDayLabel.add("2PM")
                90 -> xAxisTimeOfDayLabel.add("3PM")
                96 -> xAxisTimeOfDayLabel.add("4PM")
                102 -> xAxisTimeOfDayLabel.add("5PM")
                108 -> xAxisTimeOfDayLabel.add("6PM")
                114 -> xAxisTimeOfDayLabel.add("7PM")
                120 -> xAxisTimeOfDayLabel.add("8PM")
                126 -> xAxisTimeOfDayLabel.add("9PM")
                132 -> xAxisTimeOfDayLabel.add("10PM")
                138 -> xAxisTimeOfDayLabel.add("11PM")
                else -> xAxisTimeOfDayLabel.add("")//java.lang.String.format(Locale.US, "%02d", i) + ":00")
            }

        }
    }

    private fun clearAllChartData() {
        viewModel.clearChartArrays()
    }

    private fun isLoggedInFitbit(): Boolean {
        return (Utils.getSharedPrefs().contains("authorization_code") &&
                Utils.getSharedPrefs().contains("state") &&
                Utils.getSharedPrefs().contains("access_token"))
    }

    private fun updateChart(
        stats: List<BarEntry>, label: String,
        description: String, chart: BarChart,
    ) {
        val label1 = ArrayList<String>()
        for (i in stats) {
            label1.add(i.x.toInt().toString())
        }
        val v1: BarDataSet = BarDataSet(stats, label)
        v1.setDrawValues(true)
        v1.label = label
        if (chart == barChart2) {
            v1.color = mutedPurple
        } else {
            v1.color = lightPurple
        }
        val data = BarData()
        data.addDataSet(v1)
        configureBarChart(chart!!, description, xAxisTimeOfDayLabel)
        prepareChartData(chart!!, data)
    }



    private fun updateStackedChart(
        stats: List<SleepDataForChart>, label: String,
        description: String,
    ) {
        val labels = ArrayList<String>()
        val datasetList = ArrayList<BarEntry>()
        // Create a HashMap to store label-color mappings

        // Create a custom color array large enough to accommodate all possible entries
        val customColors = ArrayList<Int>()
        val colors = mutableListOf(mutedPurple,teal,lightPurple,darkPurple)
        for (i in stats) {
            labels.add(i.date)
            datasetList.add(i.entry)
        }
        val v1: BarDataSet = BarDataSet(datasetList, label)
        v1.setDrawValues(false)
        v1.stackLabels = arrayOf("deep", "light", "rem", "wake")
        v1.colors = colors

        val data = BarData(v1)
        // data.addDataSet(v1)
        configureBarChart(stackedBarChart!!, description, labels)
        prepareChartData(stackedBarChart!!, data)
    }

    override fun onResume() {
        super.onResume()
    }
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        //tvX!!.text = seekBarX!!.progress.toString()
        //tvY!!.text = seekBarY!!.progress.toString()

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

        if (chart == stackedBarChart) {
            chart.setVisibleXRangeMaximum(7f)
        } else {
            if (chart == barChart2) {
                data.setValueFormatter(MyValueFormatter())
            }
            else {
                data.setValueFormatter(MyValueFormatter2())
            }
            chart.setVisibleXRangeMaximum(15f)
        }
        chart.invalidate()
    }

    private fun configureBarChart(chart: BarChart, description: String, xAxisValues: ArrayList<String>) {
        //chart!!.setBackgroundColor(R.color.white)
        //chart!!.setDrawBarShadow(false)
        chart!!.setDrawGridBackground(false)

        chart!!.description.isEnabled = true
        chart!!.description.text = description
        //chart!!.description.textColor = R.color.dark_purple
        val leftAxis = chart.axisLeft
        if (chart == barChart2) {
            leftAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString() // Format the float value as an integer
                }
            }
        }



        val xAxis = chart!!.xAxis
        //xAxis.labelCount = 12
        xAxis.setDrawLabels(true)
        if (chart == barChart2 || chart == barChart1) {
            chart.setDrawValueAboveBar(true)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xAxisTimeOfDayLabel[value.toInt()]
                }
            }
        }

        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM


        xAxis.axisMinimum = 0 + 0.5f; //to center the bars inside the vertical grid lines we need + 0.5 step
        xAxis.axisMaximum = xAxisValues.size + 0.5f; //to center the bars inside the vertical grid lines we need + 0.5 step
        //xAxis.setLabelCount(12, false); //show only 5 labels (5 vertical grid lines)
        xAxis.xOffset = 0f; //labels x offset in dps
        xAxis.yOffset = 0f; //labels y offset in dps
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisValues)
        chart!!.xAxis.isEnabled = true
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
        //val markerView = CustomMarker(Graph.appContext, R.layout.marker_view)
        //chart!!.marker = markerView
    }


    override fun onNothingSelected() {}

    private class MyValueFormatter: ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value > 0) {
                value.toInt().toString() // Format the float value as an integer
            } else {
                ""
            }
        }
    }

    private class MyValueFormatter2: ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value > 0) {
                val s = value.toString()
                val clippedString = s.substring(0, s.length.coerceAtMost(4))
                clippedString // Format the float value as an integer
            } else {
                ""
            }
        }
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
