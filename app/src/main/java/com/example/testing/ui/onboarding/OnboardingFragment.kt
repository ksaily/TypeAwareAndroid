package com.example.testing.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.testing.R
import com.example.testing.databinding.FragmentOnboardingBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
