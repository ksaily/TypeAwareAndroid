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
    private var accessibilityPref : Preference? = null
    private var batteryPref : Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        Log.d("Prefs", preferenceManager.sharedPreferencesName)
        accessibilityPref = findPref(R.string.access_permission_key)
        batteryPref = findPref(R.string.battery_opt_key)

        Utils.checkAccessibilityPermission(Graph.appContext, false)
        Utils.isIgnoringBatteryOptimizations(Graph.appContext)

        setAccessibilityPref(accessibilityPref)
        setBatteryOptPref(batteryPref)

        var accessibilityBtn: Preference? = findPref(R.string.access_permission_key)
        accessibilityBtn?.setOnPreferenceClickListener {
            //construct intent to change permission
            Utils.checkAccessibilityPermission(Graph.appContext, true)
            true
        }

        var batteryBtn: Preference? = findPref(R.string.battery_opt_key)
        batteryBtn?.setOnPreferenceClickListener {
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
        Log.d("Prefs", "onResume")
        //preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        Utils.isIgnoringBatteryOptimizations(Graph.appContext)
        Utils.checkAccessibilityPermission(Graph.appContext, false)
    }

    override fun onDetach()
     {
        super.onDetach()
         Log.d("Prefs", "onDetach")
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        Log.d("Prefs", "OnSharedPrefChanged")
        if (key == "accessibility_permission") {
            Log.d("CheckPref", "Accessibility")
            setAccessibilityPref(accessibilityPref)
        }

        if (key == "battery_opt_off") {
            Log.d("CheckPref", "Battery")
            setBatteryOptPref(batteryPref)
        }
    }

    private fun setAccessibilityPref(pref: Preference?) {
        if (Utils.readSharedSettingBoolean(Graph.appContext,
                "accessibility_permission", false)) {
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

    private fun setBatteryOptPref(pref: Preference?) {
        if (Utils.readSharedSettingBoolean(Graph.appContext,
                "battery_opt_off", false)) {
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

    private fun findPref(resourceId: Int): Preference? {
        return preferenceManager.findPreference(getString(resourceId))
    }
}
