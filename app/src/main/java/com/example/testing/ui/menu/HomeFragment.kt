package com.example.testing.ui.menu

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.data.SleepData
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.checkAccessibilityPermission
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(R.layout.fragment_home), OnSharedPreferenceChangeListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentDate = ""
    var dateFragment = DateFragment()
    private val dateViewModel: DateViewModel by activityViewModels()
    private val firebaseViewModel: FirebaseViewModel by activityViewModels()
    var data = SleepData(false, 0, "", "", HashMap<String, Any>())

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "access_token") {
            checkFitbitLogin()
        }
        if (key == getString(R.string.sharedpref_accessibility)) {
            updateKeyboardData()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!dateFragment.isAdded) {
            val transaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.dateContainer, dateFragment, "dateFragment")
                .addToBackStack("dateFragment").commit()
        }

        val sharedPrefs = Utils.getSharedPrefs()

        binding.keyboardChart.dataAvailable.isVisible = false
        binding.keyboardChart.keyboardDataNotFound.isVisible = true
        checkFitbitLogin()

        binding.keyboardChart.openAccessibilitySettingsBtn.setOnClickListener {
            checkAccessibilityPermission(Graph.appContext, true)
        }

        firebaseViewModel.keyboardData.observe(viewLifecycleOwner) {
            setFirebaseDataToUI()
        }

        binding.sleepDataContainer.FitbitBtn.setOnClickListener {
            val intent = Intent(activity, AuthenticationActivity::class.java)
            startActivity(intent)
        }

        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            //firebaseViewModel.clearListOfFirebaseData()
            updateKeyboardData()
            checkFitbitLogin()
            currentDate = dateViewModel.selectedDate.value.toString()
        }

        firebaseViewModel.sleepData.observe(viewLifecycleOwner) {
            hideFitbitLogin(firebaseViewModel.sleepData.value!!)
        }
        updateKeyboardData()

        sharedPrefs.registerOnSharedPreferenceChangeListener(this)

    }

    private fun checkFitbitLogin() {
        if (!isLoggedInFitbit()) {
            showFitbitLogin()
        } else {
            updateSleepData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLoggedInFitbit()) {
            showFitbitLogin()
        } else {
            binding.sleepDataContainer.apply {
                SleepDataView.isVisible = true
                FitbitLoginVisible.isVisible = false
                sleepData.isVisible = false
                sleepDataNotFound.isVisible = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Remove observers?
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.getSharedPrefs().unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun showFitbitLogin() {
        binding.sleepDataContainer.FitbitLoginVisible.isVisible = true
        binding.sleepDataContainer.sleepData.isVisible = false
    }

    private fun updateViewWithSleepData() {
        val formattedDate = Utils.formatForFitbit(
            dateViewModel.selectedDate.value.toString()
        )
        firebaseViewModel.getSleepData(formattedDate)
    }

    private fun hideFitbitLogin(data: SleepData) {
        binding.sleepDataContainer.apply {
            SleepDataViewHidden.isVisible = false
            SleepDataView.isVisible = true
            FitbitLoginVisible.isVisible = false
            if (data.dataAvailable) {
                sleepData.isVisible = true
                sleepDataNotFound.isVisible = false
                wakeUpTime.text = data.endTime.toString()
                bedTime.text = data.startTime.toString()
                val t: Int = data.totalMinutesAsleep
                val hours = t / 60
                val minutes = t % 60
                val asleepString = "$hours h $minutes m"
                sleepAmount.text = asleepString
                /**
                val participantId = readSharedSettingString("p_id", "")
                firebaseViewModel.saveSleepDataToFirebase(
                    dateViewModel.selectedDate.value.toString(),
                    data,
                    participantId.toString()
                )**/
            } else {
                Log.d("HomeFragment", "No sleep data available")
                sleepData.isVisible = false
                sleepDataNotFound.isVisible = true
            }
        }
    }


    private fun updateSleepData() {
        if (isLoggedInFitbit() &&
                checkSleepDataSetting())
        {
            updateViewWithSleepData()
        } else if (!isLoggedInFitbit() &&
            checkSleepDataSetting())
        {
            binding.sleepDataContainer.apply {
                FitbitLoginVisible.isVisible = true
                sleepData.isVisible = false
            }
        }
    }

    private fun isLoggedInFitbit(): Boolean {
        return (Utils.getSharedPrefs().contains("authorization_code") &&
                Utils.getSharedPrefs().contains("state") &&
                Utils.getSharedPrefs().contains("access_token"))
    }

    /**
     * Checks whether 'show sleep data' is on or off
     * returns true if on and false sleep data will not be shown
     */
    private fun checkSleepDataSetting(): Boolean {
        return if (!readSharedSettingBoolean(getString(R.string.sleep_data_key), true)) {
            //Hide sleep data
            binding.sleepDataContainer.apply {
                SleepDataView.isVisible = false
                SleepDataViewHidden.isVisible = true
            }
            false
        } else {
            binding.sleepDataContainer.apply {
                SleepDataViewHidden.isVisible = false
                SleepDataView.isVisible = true
            }
            true
        }
    }


    private fun updateKeyboardData() {
        dateViewModel.checkDate()
        val isToday = dateViewModel.isToday.value!!
        val selectedDate = dateViewModel.selectedDate.value.toString()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("Entering getfromfirebase")

                firebaseViewModel.getFromFirebase(selectedDate, isToday)
            } catch (e: Exception) {
                Log.d("Error", "$e")
            }
        }
    }

    private fun setFirebaseDataToUI() {
        if (!firebaseViewModel.keyboardData.value.isNullOrEmpty()) {
            val totalErr = mutableListOf<Double>()
            val totalSpeed = mutableListOf<Double>()
            val totalErrRate = mutableListOf<Double>()
            val wordsPerMinute = mutableListOf<Double>()
            for (i in firebaseViewModel.keyboardData.value!!) {
                if (i.date == dateViewModel.selectedDate.value) {
                    totalErr.add(i.errors)
                    totalSpeed.add(i.speed)
                    totalErrRate.add(i.errorRate)
                    val wordsPM = i.averageWPM
                    wordsPerMinute.add(wordsPM)
                }
            }

            binding.keyboardChart.keyboardDataNotFound.isVisible = false
            binding.keyboardChart.dataAvailable.isVisible = true
            val clippedStringWPM: String
            val clippedStringSpeed: String
            val avgWPM = wordsPerMinute.average()
            if (avgWPM.isFinite()) {
                val s = avgWPM.toString() //words per minute
                clippedStringWPM = s.substring(0, s.length.coerceAtMost(4)) + "wpm"
                binding.keyboardChart.textViewStats.isVisible = true
                if (wordsPerMinute.average() > 25.0) {
                    binding.keyboardChart.textViewStats.text =
                        getString(R.string.home_keyboard_wpm_stats_faster_than_avg)
                } else {
                    binding.keyboardChart.textViewStats.text =
                        getString(R.string.home_keyboard_wpm_stats_slower_than_avg)
                }
            } else {
                clippedStringWPM = "- WPM"
            }
            val speedAvg = totalSpeed.average()
            if (speedAvg.isFinite()) {
                binding.keyboardChart.textViewStats.isVisible = true
                val s = speedAvg.toString() //words per minute
                clippedStringSpeed = s.substring(0, s.length.coerceAtMost(4)) + "s"
            }
            else {
                clippedStringSpeed = "- s"
                //binding.keyboardChart.textViewStats.isVisible = false
            }

            binding.keyboardChart.speedDataSeconds.text = clippedStringSpeed
            binding.keyboardChart.speedDataWPM.text = clippedStringWPM
            binding.keyboardChart.ProgressTextView.text =
                showPercentage(totalErrRate.average(),
                    binding.keyboardChart.progressCircular).toString() + "%"
        } else {

            binding.keyboardChart.dataAvailable.isVisible = false
            checkAccessibilityEnabled()
            Log.d("UpdateUI", "No data on keyboardList")
        }
    }

    private fun checkAccessibilityEnabled() {
        binding.keyboardChart.keyboardDataNotFound.isVisible = true
        if (checkAccessibilityPermission(Graph.appContext, false)) {
            binding.keyboardChart.checkAccessibilitySettingsPrompt.isVisible = false
            binding.keyboardChart.openAccessibilitySettingsBtn.isVisible = false
            binding.keyboardChart.waitForDataToUpdate.isVisible = true
        } else {
            binding.keyboardChart.checkAccessibilitySettingsPrompt.isVisible = true
            binding.keyboardChart.openAccessibilitySettingsBtn.isVisible = true
            binding.keyboardChart.waitForDataToUpdate.isVisible = false

        }
    }

    fun showPercentage(errorRate: Double, progressBar: ProgressBar): Int {
        val successRate = (1.0 - errorRate) * 100
        progressBar.progress = successRate.toInt()
        return successRate.toInt()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }


    }
}