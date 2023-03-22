package com.example.testing.ui.menu

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.FragmentDateBinding
import com.example.testing.databinding.FragmentSettingsBinding
import com.example.testing.utils.Utils

/**
 * Fragment to show and edit user preferences
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        var accessibilityBtn: Preference = preferenceManager.findPreference(getString(R.string.access_permission_key))!!
        accessibilityBtn.setOnPreferenceClickListener {
            //construct intent to change permission
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // request permission via start activity for result
                startActivity(intent)
                true
            }
        var batteryBtn: Preference = preferenceManager.findPreference(getString(R.string.battery_opt_key))!!
        batteryBtn.setOnPreferenceClickListener {
            Utils.checkBattery(Graph.appContext)
            true
        }
    }
}
