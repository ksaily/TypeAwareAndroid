package com.example.testing.ui

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.testing.R
import com.example.testing.databinding.ActivityChartBinding
import com.example.testing.ui.menu.ChartFragment
import com.example.testing.ui.menu.HomeFragment
import com.example.testing.ui.menu.SettingsFragment

class ChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChartBinding
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = findViewById(R.id.progress_circular)
        loadFragment(HomeFragment())
        bottomNav = findViewById(R.id.bottomNav)
        showPercentage(0.2, progressBar)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.settingsFragment -> {
                    loadFragment(SettingsFragment())
                    true
                }
                R.id.chartFragment -> {
                    loadFragment(ChartFragment())
                    true
                }
                else -> {false}
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }

    fun showPercentage(errorRate: Double, progressBar: ProgressBar) {
        var successRate = (1.0 - errorRate) * 100
        progressBar.progress = successRate.toInt()
        findViewById<TextView>(R.id.ProgressTextView).text = successRate.toString()
    }




}