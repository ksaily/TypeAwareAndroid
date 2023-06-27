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
                Log.d("Fragment", "Loading fragment")
                if (fragment == null) { return }
                if (bundle != null) { fragment.arguments = bundle }

                var transaction = activity.supportFragmentManager.beginTransaction()

                if (addToBackStack) { transaction.addToBackStack(tag) }
                if (!fragment.isVisible) {
                    Log.d("Fragment", "Fragment is not visible)")
                    transaction.replace(R.id.container, fragment, tag).commit()
                }
            } catch (e: java.lang.IllegalStateException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadFullScreenFragment(activity: AppCompatActivity, fragment: Fragment, bundle: Bundle?, tag: String, addToBackStack: Boolean) {
            try {
                Log.d("Fragment", "Loading fragment")
                if (fragment == null) { return }
                if (bundle != null) { fragment.arguments = bundle }

                val transaction = activity.supportFragmentManager.beginTransaction()

                if (addToBackStack) { transaction.addToBackStack(tag) }
                if (!fragment.isAdded) {
                    Log.d("Fragment", "Fragment added)")
                    transaction.replace(R.id.fullscreen_container, fragment, tag).commit()
                }
            } catch (e: java.lang.IllegalStateException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadChildFragment(child: FragmentManager, container: Int, fragment: Fragment, tag: String, addToBackStack: Boolean) {
            try {
                Log.d("Fragment", "Loading fragment")
                if (fragment == null) { return }

                val transaction = child.beginTransaction()

                if (addToBackStack) { transaction.addToBackStack(tag) }
                if (!fragment.isAdded) {
                    Log.d("Fragment", "$fragment added")
                    transaction.replace(container, fragment, tag).commit()
                }
            } catch (e: java.lang.IllegalStateException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Remove fragment from container
         *
         * @param activity to get FragmentManager
         * @param tag tag of fragment to remove
         */
        fun removeFragmentFromContainer(activity: AppCompatActivity, tag: String) {
            var transaction = activity.supportFragmentManager.beginTransaction()
            if (transaction != null) {
                activity.supportFragmentManager.findFragmentByTag(tag)
                    ?.let { transaction.remove(it) }
                transaction.commit()
            }
        }

        fun isFragmentInStack(activity: AppCompatActivity, tag: String): Boolean {
            var inStack = false
            var fragmentManager = activity.supportFragmentManager
            var fragment = fragmentManager.findFragmentByTag(tag)
            if (fragment != null) {
                inStack = true
            }
            return inStack
        }

        fun getFragmentByTag(activity: AppCompatActivity, tag: String): Fragment? {
            var fragmentManager = activity.supportFragmentManager
            var fragment = fragmentManager.findFragmentByTag(tag)
            if (fragment != null) {
                return fragment
            }
            return null
        }

        fun removeFragmentByTag(activity: AppCompatActivity, tag: String) {
            var fragmentManager = activity.supportFragmentManager
            if (fragmentManager != null) {
                var fragment = fragmentManager.findFragmentByTag(tag)
                if (fragment != null) {
                    fragmentManager.beginTransaction().remove(fragment).commit()
                }
            }
        }
    }
}