package com.example.testing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.testing.utils.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.currentCoroutineContext

/**
 * ViewModel for returning Firebase data when updated
 */
class FirebaseViewModel(application: Application): AndroidViewModel(application) {

    private val p_id = Utils.readSharedSettingString(getApplication<Application>()
        .applicationContext, "p_id", "").toString()
    private val firebaseRef: DatabaseReference =
        FirebaseDatabase.getInstance().reference.child(p_id)


    companion object {
        private val liveData: FireBaseQueryLiveData = FireBaseQueryLiveData()
        fun getSnapshotLiveData(): LiveData<DataSnapshot> {
            return liveData
        }
    }
}