package com.example.testing.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.testing.R
import com.example.testing.databinding.FragmentUserInfoBinding
import com.example.testing.ui.menu.DateFragment

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
            //Save user info to shared pref
            var p_id = binding.participantId.text.toString()
            var username = binding.textView14.text.toString()
            var email = binding.userEmail.text.toString()
            setFragmentResult("userInfo", bundleOf("username" to username,
            "email" to email, "p_id" to p_id))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        fun newInstance() = UserInfoFragment()
    }
}