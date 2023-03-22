package com.example.testing.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.ActivityOnboardingBinding
import com.example.testing.ui.viewmodel.PrefsViewModel
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.showSnackbar
import com.google.android.material.snackbar.Snackbar

private const val NUM_PAGES = 3

/**
 * Activity to show onboarding screens
 * Reference: https://github.com/andrea-liu87/Ikigai
 */
class OnboardingActivity : FragmentActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var view: View
    var onboardingDone: Boolean = false

    private var onBoardingPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateCircleMarker(binding, position)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.vp2Pager.adapter = pagerAdapter
        binding.vp2Pager.registerOnPageChangeCallback(onBoardingPageChangeCallback)
        binding.next.setOnClickListener {
            if (!Utils.readSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete", false)
            ) {
                binding.vp2Pager.currentItem = binding.vp2Pager.currentItem + 1
            } else {
                Utils.saveSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete", true)
            }
        }

        binding.skip.setOnClickListener {
            view.showSnackbar(view, getString(R.string.skip_prompt), Snackbar.LENGTH_INDEFINITE,
                getString(R.string.skip)) {
                Utils.saveSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete",true)
            }
        }

        //Register listener
        Utils.getSharedPrefs().registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "onboarding_complete") {
                if (Utils.readSharedSettingBoolean(
                        Graph.appContext, "onboarding_complete", false)
                ) {
                    Log.d("OnboardingActivity", "Start mainActivity")
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.vp2Pager.unregisterOnPageChangeCallback(onBoardingPageChangeCallback)
    }

    override fun onBackPressed() {
        if (binding.vp2Pager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.vp2Pager.currentItem = binding.vp2Pager.currentItem - 1
        }
    }

    /**
     * Update slider circle view based on fragment position
     */
    private fun updateCircleMarker(binding: ActivityOnboardingBinding, position: Int) {
        when (position) {
            0 -> {
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
            }
            1 -> {
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
            }
            2 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.next.text = "Finish"
                binding.skip.visibility = INVISIBLE
                Utils.saveSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete",true)
            }
            3 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
            }
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) :
        FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
            OnboardingFragment.getInstance(position)
    }


}