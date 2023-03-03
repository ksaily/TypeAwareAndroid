package com.example.testing.ui.view

import android.app.Application
import android.inputmethodservice.Keyboard
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.testing.utils.KeyboardStats
import com.example.testing.utils.Utils
import com.google.firebase.database.*

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