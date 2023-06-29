package com.example.testing.charts
import android.content.Context
import com.example.testing.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.testing.R.layout.marker_view
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.databinding.MarkerViewBinding

class CustomMarker(context: Context, layoutResource: Int = R.layout.marker_view):  MarkerView(context, layoutResource) {
    private var _binding: MarkerViewBinding? = null
    private val binding get() = _binding!!


    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        val value = entry?.y?.toDouble() ?: 0.0
        var resText = ""
        resText = if(value.toString().length > 8){
            value.toString().substring(0,7)
        } else{
            "$value"
        }
        binding.tvPrice.text = resText
        super.refreshContent(entry, highlight)
    }

    override fun getOffsetForDrawingAtPoint(xpos: Float, ypos: Float): MPPointF {
        return MPPointF(-width / 2f, -height - 10f)
    }
}