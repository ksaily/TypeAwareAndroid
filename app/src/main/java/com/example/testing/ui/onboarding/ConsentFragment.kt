package com.example.testing.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import androidx.fragment.app.setFragmentResult
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentConsentBinding
import com.example.testing.ui.menu.DateFragment
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.FragmentUtils.Companion.removeFragmentByTag
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.showSnackbar
import com.google.android.material.snackbar.Snackbar

class ConsentFragment : Fragment(R.layout.fragment_consent) {

    private var _binding: FragmentConsentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentConsentBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * If consent for data collection is given, save the consent to shared preferences
     * and close this fragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var result = false
        val sharedPrefs = Utils.getSharedPrefs()

        binding.conBtnGiveConsent.setOnClickListener {
            result = true
            Log.d("Consent", "Consent given")
            //setFragmentResult("consentGiven", bundleOf("consent" to result))
            Utils.saveSharedSettingBoolean("consent_given", true)

              //  null, "userInfoFragment", false)
            //removeFragmentByTag(MainActivity, "consentFragment")
            //parentFragmentManager.beginTransaction().replace(R.id.container, UserInfoFragment())
            //parentFragmentManager.popBackStack()
        }

        binding.conBtnQuit.setOnClickListener {
            view.showSnackbar(
                view, getString(R.string.con_not_now_prompt),
                Snackbar.LENGTH_SHORT, "Continue"
            ) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = ConsentFragment()
    }



}