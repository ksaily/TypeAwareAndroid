package com.example.testing.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.testing.R
import com.example.testing.databinding.FragmentOnboardingItemBinding

class OnboardingItemFragment: Fragment(R.layout.fragment_onboarding_item) {

    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun getInstance(position: Int) = OnboardingItemFragment().apply {
            arguments = bundleOf(ARG_POSITION to position)
        }
    }

    private var _binding: FragmentOnboardingItemBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = FragmentOnboardingItemBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val position = requireArguments().getInt(OnboardingItemFragment.ARG_POSITION)
        val onBoardingTitles = requireContext().resources.getStringArray(R.array.onboarding_titles)
        val onBoardingTexts = requireContext().resources.getStringArray(R.array.onboarding_texts)
        with(binding)  {
            onboardingTitle.text = onBoardingTitles[position]
            onboardingContent.text = onBoardingTexts[position]
            when (position) {
                0 -> {
                    onboardingImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.coffee_break_02_compressed
                        )
                    )
                }
                1 -> {
                    onboardingImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.sleep_screen
                        )
                    )
                }
                2 -> {
                    onboardingImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.time_management
                        )
                    )
                }
                3 -> {
                    onboardingImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.screenshot_prefs__2_ //replace with picture of settings
                    ))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}