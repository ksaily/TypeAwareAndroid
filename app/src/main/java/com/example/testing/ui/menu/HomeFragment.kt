package com.example.testing.ui.menu

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

// TODO: Rename parameter arguments, choose names that match
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
    private var chart: BarChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Orders", "Inventory"
    )
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        chart = binding.barChart
        binding.FitbitBtn.setOnClickListener {
            val intent = Intent(activity, AuthenticationActivity::class.java)
            startActivity(intent)
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
        configureBarChart()
        prepareChartData(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun prepareChartData(data: BarData) {
        chart!!.data = data
        chart!!.barData.barWidth = BAR_WIDTH
        val groupSpace = 1f - (BAR_SPACE + BAR_WIDTH)
        chart!!.groupBars(0f, groupSpace, BAR_SPACE)
        chart!!.invalidate()
    }

    private fun configureBarChart() {
        chart!!.setPinchZoom(false)
        chart!!.setDrawBarShadow(false)
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
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