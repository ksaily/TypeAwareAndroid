package com.example.testing.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import com.example.testing.R
import com.example.testing.databinding.FragmentDateBinding
import com.example.testing.databinding.FragmentSettingsBinding

/**
 * Fragment to show and edit user preferences
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}