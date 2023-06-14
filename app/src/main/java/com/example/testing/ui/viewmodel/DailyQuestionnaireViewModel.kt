package com.example.testing.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.testing.ui.data.KeyboardChart

class DailyQuestionnaireViewModel: ViewModel() {

    private val _isQuestionnaireAnswered = MutableLiveData<Boolean>()
    val isQuestionnaireAnswered: LiveData<Boolean>
        get() = _isQuestionnaireAnswered

    fun setIsQuestionnaireAnswered(bool: Boolean) {
        _isQuestionnaireAnswered.value = bool
    }
}