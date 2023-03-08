package com.example.testing.ui.onboarding

import android.os.Bundle
import android.content.Context
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.os.bundleOf
import com.example.testing.R
import com.example.testing.databinding.FragmentOnboardingBinding

/**
 * Fragment to show onboarding information
 * Image credits:
 *
 * <a href="https://www.freepik.com/free-vector/man-moving-clock-arrows-managing-time_11235693.
 * htm#query=sleep&position=27&from_view=search&track=sph">Image by pch.vector</a> on Freepik
 *
 *
 */
class OnboardingFragment : Fragment() {

    companion object {
        private const val ARG_POSITION = "ARG_POSITION"

        fun getInstance(position: Int) = OnboardingFragment().apply {
            arguments = bundleOf(ARG_POSITION to position)
        }
    }

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val position = requireArguments().getInt(ARG_POSITION)
        val onBoardingTitles = requireContext().resources.getStringArray(R.array.onboarding_titles)
        val onBoardingTexts = requireContext().resources.getStringArray(R.array.onboarding_texts)
        with(binding)  {
            onboardingTitle.text = onBoardingTitles[position]
            onboardingContent.text = onBoardingTexts[position]
            when (position) {
                0 -> {
                    onboardingImage.setImageDrawable(getDrawable(requireContext(),R.drawable.coffee_break_02_compressed))
                }
                1 -> {
                    onboardingImage.setImageDrawable(getDrawable(requireContext(),R.drawable.sleep_screen))
                }
                2 -> {
                    onboardingImage.setImageDrawable(getDrawable(requireContext(),R.drawable.time_management))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
