package com.example.testing.ui.menu

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.datastore.preferences.preferencesKey
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.FragmentDateBinding
import com.example.testing.databinding.FragmentSettingsBinding
import com.example.testing.utils.Utils

/**
 * Fragment to show and edit user preferences
 */
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        Log.d("Prefs", preferenceManager.sharedPreferencesName)
        var accessibilityBtn: Preference = preferenceManager.findPreference(getString(R.string.access_permission_key))!!
        accessibilityBtn.setOnPreferenceClickListener {
            //construct intent to change permission
            Log.d("Prefs", "Check accessibility")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // request permission via start activity for result
            Graph.appContext.startActivity(intent)
            true
        }
        var batteryBtn: Preference = preferenceManager.findPreference(getString(R.string.battery_opt_key))!!

        batteryBtn.setOnPreferenceClickListener {
            Log.d("Prefs", "Check battery")
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            Graph.appContext.startActivity(intent)
            Utils.isIgnoringBatteryOptimizations(Graph.appContext)
            true
        }
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        Utils.isIgnoringBatteryOptimizations(Graph.appContext)
        Utils.checkAccessibilityPermission(Graph.appContext, false)
    }

    override fun onDestroy()
     {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d("Prefs", "OnSharedPrefChanged")
        if (key == "accessibility_permission") {
            val pref = findPref(R.string.access_permission_key)
            if (Utils.readSharedSettingBoolean(Graph.appContext, key, false)) {
                pref?.title = getString(R.string.accessibility_perm_enabled)
                pref?.summary = getString(R.string.click_here_to_make_changes)
                pref?.icon = Graph.appContext.getDrawable(
                    R.drawable.ic_baseline_check_circle_outline_24)
            } else {
                pref?.title = getString(R.string.accessibility_perm_disabled)
                pref?.summary = getString(R.string.accessibility_permission_change)
                pref?.icon = Graph.appContext.getDrawable(
                    R.drawable.ic_outline_error_outline_24)
            }
        }

        if (key == "battery_opt_off") {
            val pref = findPref(R.string.access_permission_key)
            if (Utils.readSharedSettingBoolean(Graph.appContext, key, false)) {
                pref?.title = getString(R.string.battery_opt_off_prompt)
                pref?.summary = getString(R.string.click_here_to_make_changes)
                pref?.icon = Graph.appContext.getDrawable(
                    R.drawable.ic_baseline_check_circle_outline_24)
            } else {
                pref?.title = getString(R.string.battery_opt_on_prompt)
                pref?.summary = getString(R.string.battery_optimization_prompt)
                pref?.icon = Graph.appContext.getDrawable(
                    R.drawable.ic_outline_error_outline_24)
            }
        }
    }

    private fun findPref(resourceId: Int): Preference? {
        return preferenceManager.findPreference(getString(resourceId))
    }
}
