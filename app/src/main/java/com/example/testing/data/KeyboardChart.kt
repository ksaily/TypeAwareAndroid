package com.example.testing.data

import com.github.mikephil.charting.data.BarEntry

data class KeyboardChart(
    val timeWindow: Int,
    val sessions: MutableList<BarEntry>,
    val errorAmount: MutableList<BarEntry>,
    val speed: Double
)
