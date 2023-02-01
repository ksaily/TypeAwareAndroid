package com.example.testing.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.testing.R
import com.example.testing.databinding.ActivityOnboardingBinding

private const val NUM_PAGES = 3

/**
 * Activity to show onboarding screens
 * Reference: https://github.com/andrea-liu87/Ikigai
 */
class OnboardingActivity : FragmentActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var view: View

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
    }

    override fun onDestroy() {
        binding.vp2Pager.unregisterOnPageChangeCallback(onBoardingPageChangeCallback)
        super.onDestroy()
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
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
            }
            1 -> {
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
            }
            2 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivFirstCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
            }
            3 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_purple))
                binding.ivSecondCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
                binding.ivThirdCircle.setImageDrawable(getDrawable(R.drawable.comp_view_circle_gray))
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