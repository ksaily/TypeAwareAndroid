package com.example.testing.charts

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.example.testing.R
import com.example.testing.databinding.FragmentChartBinding
import com.example.testing.databinding.FragmentStackedBarChartBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StackedBarChartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StackedBarChartFragment : Fragment(), SeekBar.OnSeekBarChangeListener,
    OnChartValueSelectedListener {
    private var param1: String? = null
    private var param2: String? = null
    private var chart: BarChart? = null
    private var seekBarX: SeekBar? = null
    private var seekBarY: SeekBar? = null
    private var tvX: TextView? = null
    private var tvY: TextView? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //chart = binding.StackedBarChart
        //tvX = findViewById(android.R.id.tvXMax)
        //tvY = findViewById(android.R.id.tvYMax)
        //seekBarX = findViewById(android.R.id.seekBar1)
        //seekBarX!!.setOnSeekBarChangeListener(this)
        //seekBarY = findViewById(android.R.id.seekBar2)
        //seekBarY!!.setOnSeekBarChangeListener(this)
        chart!!.setOnChartValueSelectedListener(this)
        chart!!.description.isEnabled = false

        // if more than 60 entries are displayed in the chart, no values will be
        // drawn
        chart!!.setMaxVisibleValueCount(8)

        // scaling can now only be done on x- and y-axis separately
        chart!!.setPinchZoom(false)
        chart!!.setDrawGridBackground(false)
        chart!!.setDrawBarShadow(false)
        chart!!.setDrawValueAboveBar(false)
        chart!!.isHighlightFullBarEnabled = false

        // change the position of the y-labels
        val leftAxis = chart!!.axisLeft
        //leftAxis.valueFormatter = MyAxisValueFormatter()
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        chart!!.axisRight.isEnabled = false
        val xLabels = chart!!.xAxis
        xLabels.position = XAxis.XAxisPosition.TOP

        // chart.setDrawXLabels(false);
        // chart.setDrawYLabels(false);

        // setting data
        seekBarX!!.progress = 12
        seekBarY!!.progress = 100
        val l = chart!!.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.HORIZONTAL
        l.setDrawInside(false)
        l.formSize = 8f
        l.formToTextSpace = 4f
        l.xEntrySpace = 6f

        // chart.setDrawLegend(false);
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        //tvX!!.text = seekBarX!!.progress.toString()
        //tvY!!.text = seekBarY!!.progress.toString()
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
        chart!!.invalidate()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onValueSelected(e: Entry, h: Highlight) {
        val entry = e as BarEntry
        if (entry.yVals != null) Log.i("VAL SELECTED",
            "Value: " + entry.yVals[h.stackIndex]) else Log.i("VAL SELECTED", "Value: " + entry.y)
    }

    override fun onNothingSelected() {}


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param date Currently selected date.
         * @return A new instance of fragment StackedBarChartFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String) =
            StackedBarChartFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}