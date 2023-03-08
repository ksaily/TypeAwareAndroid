package com.example.testing.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testing.utils.KeyboardStats

/**
 * ViewModel for returning Firebase data when updated
 */
class FirebaseViewModel(application: Application): AndroidViewModel(application) {

    private val _keyboardData = MutableLiveData(ArrayList<KeyboardStats>())
    val keyboardData: LiveData<ArrayList<KeyboardStats>>
        get() = _keyboardData

    fun addToListOfFirebaseData(data: KeyboardStats) {
        _keyboardData.value?.add(data)
    }

    fun clearListOfFirebaseData() {
        _keyboardData.value?.clear()
    }
}