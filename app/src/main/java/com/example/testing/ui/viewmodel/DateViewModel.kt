package com.example.testing.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.currentDate

class DateViewModel : ViewModel() {
    private val _selectedDate = MutableLiveData(currentDate)
    val selectedDate: LiveData<String>
        get() = _selectedDate
    private val _isToday = MutableLiveData(true)
    val isToday: LiveData<Boolean>
        get() = _isToday

    fun currentDay() {
        _selectedDate.value = currentDate
    }

    fun nextDay() {
        _selectedDate.value = Utils.getNextDateString(_selectedDate.value!!)
    }

    fun previousDay() {
        _selectedDate.value = Utils.getPreviousDateString(_selectedDate.value!!)
    }

    fun checkDate(): String {
        return if (Utils.getNextDateString(_selectedDate.value.toString()) == currentDate) {
            _isToday.value = false
            "Yesterday"
        } else if (_selectedDate.value == currentDate) {
            _isToday.value = true
            "Today"
        } else {
            _isToday.value = false
            _selectedDate.value.toString()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("DateFragment", "DateViewModel destroyed!")
    }
}