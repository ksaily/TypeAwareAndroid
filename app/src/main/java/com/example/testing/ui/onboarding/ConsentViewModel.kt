package com.example.testing.ui.onboarding

import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event
import android.content.ClipData
import android.content.ClipData.Item
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConsentViewModel: ViewModel() {
    private val mutableSelectedItem = MutableLiveData<Item>()
    val selectedItem: LiveData<Item> get() = mutableSelectedItem

    fun selectItem(item: Item) {
        mutableSelectedItem.value = item
    }
}