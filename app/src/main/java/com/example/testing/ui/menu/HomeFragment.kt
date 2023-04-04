package com.example.testing.ui.menu

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.charts.CustomMarker
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.fitbit.FitbitApiService
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.countAvgSpeed
import com.example.testing.utils.Utils.Companion.getFromFirebase
import com.example.testing.utils.Utils.Companion.keyboardList
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {
    // Chart variables:
    private val MAX_X_VALUE = 13
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Orders"
    private val GROUP_2_LABEL = ""
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.8f
    private var lineChart: LineChart? = null
    private var chart: BarChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Orders", "Inventory"
    )
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var chosenDate: String = ""
    private val formatter = SimpleDateFormat("yyyy-MM-dd")
    var dateFragment = DateFragment()
    private val dateViewModel: DateViewModel by viewModels()
    private val firebaseViewModel: FirebaseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.dateContainer, dateFragment, "dateFragment")
            .addToBackStack("dateFragment").commit()

        if (!readSharedSettingBoolean(Graph.appContext, "loggedInFitbit", false)) {
            showFitbitLogin()
        } else {
            hideFitbitLogin()
        }

        binding.sleepDataContainer.FitbitBtn.setOnClickListener {
            val intent = Intent(activity, AuthenticationActivity::class.java)
            startActivity(intent)
        }
        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            firebaseViewModel.clearListOfFirebaseData()
            updateKeyboardData()
            updateSleepData()
        }


        var values1: ArrayList<BarEntry> = ArrayList()
        var values2: ArrayList<BarEntry> = ArrayList()
        statValues.clear()
        //
        for (i in 0 until MAX_X_VALUE) {
            values1.add(
                BarEntry(
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
        val dataSet1: BarDataSet = BarDataSet(values1, "Test")
        val dataSet2: BarDataSet = BarDataSet(values2, "Test")
        val data: BarData = BarData()
        data.addDataSet(dataSet1)
        data.addDataSet(dataSet2)
        //configureBarChart()
        //prepareChartData(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFitbitLogin() {
        binding.sleepDataContainer.FitbitBtn.isVisible = true
        binding.sleepDataContainer.FitbitLoginPrompt.isVisible = true
        binding.sleepDataContainer.hideThis.isVisible = false
        binding.sleepDataContainer.sleepData.isVisible = false
    }

    private fun hideFitbitLogin() {
        binding.sleepDataContainer.apply {
            FitbitBtn.isVisible = false
            FitbitLoginPrompt.isVisible = false
            sleepData.isVisible = true
            if (checkSleepDataSetting()) {
                lifecycleScope.launchWhenStarted {
                    val sleepData =
                        FitbitApiService.getSleepData(dateViewModel.selectedDate.value.toString())
                    if (sleepData.dataAvailable) {
                        wakeUpTime.text = sleepData.endTime
                        bedTime.text = sleepData.startTime
                        val t: Int = sleepData.totalMinutesAsleep
                        val hours = t / 60
                        val minutes = t % 60
                        val asleepString = "$hours h $minutes m"
                        sleepAmount.text = asleepString
                    } else {
                        SleepDataView.isVisible = false
                        SleepDataViewHidden.isVisible = true
                        SleepDataViewHiddenTitle.text = getString(R.string.home_sleep_data_not_found_title)
                        SleepDataViewHiddenContent.text = getString(R.string.home_sleep_data_not_found_content)

                    }
                }
            }
        }
    }

    private fun updateSleepData() {
        if (Utils.readSharedSettingBoolean(
                Graph.appContext, "loggedInFitbit", false) &&
                checkSleepDataSetting())
        {
            hideFitbitLogin()
        } else if (!Utils.readSharedSettingBoolean(
                Graph.appContext, "loggedInFitbit", false) &&
            checkSleepDataSetting())
        {
            binding.sleepDataContainer.apply {
                FitbitBtn.isVisible = true
                FitbitLoginPrompt.isVisible = true
                sleepData.isVisible = false
            }
        } else if (!checkSleepDataSetting())
        {
            binding.sleepDataContainer.apply {
                SleepDataViewHiddenTitle.text = getString(R.string.home_sleep_data_hidden_title)
                SleepDataViewHiddenContent.text = getString(R.string.home_sleep_data_hidden_content)
            }
        }
    }

    /**
     * Checks whether 'show sleep data' is on or off
     * returns true if on and false sleep data will not be shown
     */
    private fun checkSleepDataSetting(): Boolean {
        return if (!Utils.readSharedSettingBoolean(Graph.appContext,
                getString(R.string.sleep_data_key), true)) {
            //Hide sleep data
            binding.sleepDataContainer.apply {
                SleepDataView.isVisible = false
                SleepDataViewHidden.isVisible = true
            }
            false
        } else {
            binding.sleepDataContainer.apply {
                SleepDataViewHidden.isVisible = false
                sleepData.isVisible = true
            }
            true
        }
    }


    private fun updateKeyboardData() {
        lifecycleScope.launchWhenStarted {
            getFromFirebase(dateViewModel.selectedDate.value.toString())
        }
        if (keyboardList.isNotEmpty()) {
            val totalErr = mutableListOf<Double>()
            val totalSpeed = mutableListOf<Double>()
            for (i in keyboardList) {
                if (i.date == dateViewModel.selectedDate.value) {
                    totalErr.add(i.errors)
                    totalSpeed.add(i.speed)
                }
            }
            binding.keyboardChart.speedData.text = countAvgSpeed(totalSpeed).toString()
            binding.keyboardChart.ProgressTextView.text = showPercentage(countAvgSpeed(totalErr),
            binding.keyboardChart.progressCircular).toString()
        }
        else {
            binding.keyboardChart.speedData.text = "No data"
            binding.keyboardChart.ProgressTextView.text == "--"
            binding.keyboardChart.progressCircular.isVisible = false
            Log.d("UpdateUI", "No data on keyboardList")
        }
    }

    private fun prepareChartData(data: BarData) {
        chart!!.data = data
        //chart!!.barData.barWidth = BAR_WIDTH
        val groupSpace = 1f - (BAR_SPACE + BAR_WIDTH)
        //chart!!.groupBars(0f, groupSpace, BAR_SPACE)
        chart!!.invalidate()
    }

    private fun configureBarChart() {
        chart!!.setPinchZoom(false)
        //chart!!.setDrawBarShadow(false)
        chart!!.setDrawGridBackground(false)

        chart!!.description.isEnabled = false
        val xAxis = chart!!.xAxis
        xAxis.granularity = 1f
        xAxis.setCenterAxisLabels(true)
        xAxis.setDrawGridLines(false)
        val leftAxis = chart!!.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.spaceTop = 35f
        leftAxis.axisMinimum = 0f
        chart!!.axisRight.isEnabled = false
        chart!!.xAxis.axisMinimum = 1f
        chart!!.xAxis.axisMaximum = MAX_X_VALUE.toFloat()
    }

    fun showPercentage(errorRate: Double, progressBar: ProgressBar, ): Double {
        var successRate = (1.0 - errorRate) * 100
        progressBar.progress = successRate.toInt()
        return successRate
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }


    }
}