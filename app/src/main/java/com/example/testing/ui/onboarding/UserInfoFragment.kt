package com.example.testing.ui.onboarding

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.FragmentUserInfoBinding
import com.example.testing.utils.Utils

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class UserInfoFragment : Fragment(R.layout.fragment_user_info) {

    private var _binding: FragmentUserInfoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveUserInfo.setOnClickListener {
            if (!binding.participantId.text.isNullOrEmpty()) {
                //Save user info to shared pref
                var p_id = binding.participantId.text.toString()
                Utils.getSharedPrefs().edit()
                    .putString("p_id", p_id)
                    .putBoolean("user_info_saved", true)
                    .commit()
            }
            else {
                Toast.makeText(Graph.appContext, "Please enter your Prolific ID", Toast.LENGTH_SHORT)
                    .show()
            }
            //parentFragmentManager.beginTransaction().replace(R.id.container, OnboardingFragment())
            //startActivity(Intent(Graph.appContext, OnboardingActivity::class.java))
            //removeFragmentByTag(MainActivity, "userInfoFragment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


    companion object {
        fun newInstance() = UserInfoFragment()
    }
}