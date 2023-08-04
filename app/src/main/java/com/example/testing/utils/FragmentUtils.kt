package com.example.testing.utils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.testing.R
import com.example.testing.ui.menu.ChartFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Class for handling fragment events,
 * such as adding a fragment to container or removing it
 */
class FragmentUtils {
    companion object {

        /**
         * Load fragment into container
         *
         * @param activity required to get fragment manager
         * @param fragment fragment to add
         * @param bundle data to send to fragment
         * @param tag tag for fragment to add
         * @param addToBackStack if true, fragment will be added to backstack
         */
        fun loadFragment(activity: AppCompatActivity, fragment: Fragment, bundle: Bundle?, tag: String, addToBackStack: Boolean) {
            try {
                if (fragment == null) { return }
                if (bundle != null) { fragment.arguments = bundle }

                var transaction = activity.supportFragmentManager.beginTransaction()

                if (addToBackStack) { transaction.addToBackStack(tag) }
                if (!fragment.isVisible) {
                    transaction.replace(R.id.container, fragment, tag).commit()
                }
            } catch (e: java.lang.IllegalStateException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}