package com.example.testing.ui.onboarding

import android.content.ClipData.Item
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OnboardingViewModel: ViewModel() {
    private val _onboardingFinished = MutableLiveData(false)
    val onboardingFinished: LiveData<Boolean>
        get() = _onboardingFinished

    fun setOnboardingComplete(bool: Boolean) {
        _onboardingFinished.value = true
    }
}