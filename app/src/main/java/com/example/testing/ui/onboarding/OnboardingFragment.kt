package com.example.testing.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentOnboardingBinding
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.FragmentUtils.Companion.removeFragmentByTag
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.showSnackbar
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment to show onboarding information
 * Image credits:
 *
 * <a href="https://www.freepik.com/free-vector/man-moving-clock-arrows-managing-time_11235693.
 * htm#query=sleep&position=27&from_view=search&track=sph">Image by pch.vector</a> on Freepik
 *
 *
 */
private const val NUM_PAGES = 3

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun getInstance(position: Int) = OnboardingFragment().apply {
            arguments = bundleOf(ARG_POSITION to position)
        }
    }

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root

    }

    private var onBoardingPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateCircleMarker(binding, position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.vp2Pager.adapter = pagerAdapter
        binding.vp2Pager.registerOnPageChangeCallback(onBoardingPageChangeCallback)
        binding.next.setOnClickListener {
            if (onboardingViewModel.onboardingFinished.value == false) {
                binding.vp2Pager.currentItem = binding.vp2Pager.currentItem + 1
            } else {
                Utils.saveSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete", true)
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        }

        binding.skip.setOnClickListener {
            view.showSnackbar(view, getString(R.string.skip_prompt), Snackbar.LENGTH_INDEFINITE,
                getString(R.string.skip)) {
                Utils.saveSharedSettingBoolean(
                    Graph.appContext, "onboarding_complete",true)
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.vp2Pager.unregisterOnPageChangeCallback(onBoardingPageChangeCallback)
    }

    /**
     * Update slider circle view based on fragment position
     */
    private fun updateCircleMarker(binding: FragmentOnboardingBinding, position: Int) {
        when (position) {
            0 -> {
                binding.ivFirstCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
            }
            1 -> {
                binding.ivSecondCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_gray))
                binding.ivFirstCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
            }
            2 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
                binding.ivFirstCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
                binding.next.text = "Finish"
                binding.skip.visibility = View.INVISIBLE
                onboardingViewModel.setOnboardingComplete(true)
            }
            3 -> {
                binding.ivThirdCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_gray))
                binding.ivSecondCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
                binding.ivThirdCircle.setImageDrawable(getDrawable(Graph.appContext, R.drawable.comp_view_circle_purple))
            }
        }
    }

    private inner class ScreenSlidePagerAdapter(f: Fragment) :
        FragmentStateAdapter(f) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
            OnboardingItemFragment.getInstance(position)
    }
}
