package com.example.testing.ui.viewmodel

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.utils.Utils


class PrefsViewModel : ViewModel() {


    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
        Graph.appContext)

    private val _batteryOptOff = MutableLiveData(true)
    val batteryOptOff: LiveData<Boolean>
        get() = _batteryOptOff

    private val _accessibilityEnabled = MutableLiveData(true)
    val accessibilityEnabled: LiveData<Boolean>
        get() = _accessibilityEnabled

    private val _permissionsOk = MutableLiveData(true)
    val permissionsOk: LiveData<Boolean>
        get() = _permissionsOk

    private val _onboardingCompleted = MutableLiveData(false)
    val onboardingCompleted: LiveData<Boolean>
        get() = _onboardingCompleted

    private val _consentGiven = MutableLiveData(false)
    val consentGiven: LiveData<Boolean>
        get() = _consentGiven


    /**
     * Check if optimization is enabled
     */
    fun checkBatteryOptimization(context: Context) {
        _batteryOptOff.value =
            !(!isIgnoringBatteryOptimizations(context.applicationContext) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    }

    /**
     * Return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pwrm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.applicationContext.packageName
        val editor = sharedPrefs.edit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            editor.putBoolean("battery_opt_off", false)
            editor.apply()
            //_batteryOptOff.value = false
            return pwrm.isIgnoringBatteryOptimizations(name)
        }
        editor.putBoolean("battery_opt_off", true)
        editor.apply()
        //_batteryOptOff.value = true
        return true
    }

    /** Check for accessibility permissions **/
    fun checkAccessibilityPermission() {
        var accessEnabled = 0
        try {
            accessEnabled =
                Settings.Secure.getInt(Graph.appContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        sharedPrefs.edit().putBoolean("accessibility_enabled",(accessEnabled != 0)).apply()
        _accessibilityEnabled.value = (accessEnabled != 0)
    }


    fun isOnboardingCompleted(): Boolean? {
        return _onboardingCompleted.value
    }

    fun setConsentGivenValue(bool: Boolean) {
        _consentGiven.value = bool
    }

    fun isConsentGiven(): Boolean? {
        return _consentGiven.value
    }

    fun setOnboardingCompleteValue(bool: Boolean) {
        _onboardingCompleted.value = bool
    }

    fun checkPermissions() {
        checkAccessibilityPermission()
        checkBatteryOptimization(Graph.appContext)
        isOnboardingCompleted()
        isConsentGiven()
        _permissionsOk.value = _accessibilityEnabled.value == true && _batteryOptOff.value == true &&
                _consentGiven.value == true && _onboardingCompleted.value == true
    }
}