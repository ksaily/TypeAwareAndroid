package com.example.testing.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.viewpager.widget.ViewPager
import com.example.testing.R
import com.ogaclejapan.smarttablayout.SmartTabLayout

import com.example.testing.databinding.ActivityConsentBinding
import com.example.testing.ui.viewmodel.PrefsViewModel
import com.example.testing.utils.FragmentUtils.Companion.loadFragment
import com.example.testing.utils.FragmentUtils.Companion.removeFragmentByTag
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.showSnackbar

/**
 * Activity to host fragments for getting consent
 * and user information. After the information and consent is received,
 * start onboarding activity
 */
class ConsentActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityConsentBinding
    private val consentFragment = ConsentFragment()
    private val userInfoFragment = UserInfoFragment()
    private lateinit var view: View
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsentBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)
        val sharedPrefs = getSharedPreferences("USER_INFO", Context.MODE_PRIVATE)
        editor = sharedPrefs.edit()
        loadFragment(this, consentFragment, null, "consentFragment", false)
        supportFragmentManager
            .setFragmentResultListener("consentGiven", this) { requestKey, bundle ->
                var result = bundle.getBoolean("consent")
                if (result) {
                    //Consent for data collection is given
                    //Change it in shared preferences
                    editor.putBoolean("consent_given", true).commit()
                    loadFragment(this, userInfoFragment, null, "userInfoFragment", false)
                    removeFragmentByTag(this, "consentFragment")
                } else {
                    //Do nothing?
                }
            }
        supportFragmentManager.setFragmentResultListener("userInfo", this) { requestKey, bundle ->
            var name = bundle.getString("userName")
            var email = bundle.getString("email")
            var p_id = bundle.getString("p_id")
            editor.putString("username", name)
                .putString("email", email)
                .putString("p_id", p_id)
                .putBoolean("user_info_saved", true)
            editor.commit()
            //removeFragmentByTag(this, "userInfoFragment")
        }
    }

    override fun onResume() {
        super.onResume()
        if (Utils.checkPermissions(applicationContext)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

}