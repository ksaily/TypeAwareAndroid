package com.example.testing.ui.menu

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.text.trimmedLength
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.fitbit.AuthenticationActivity
import com.example.testing.fitbit.FitbitApiService
import com.example.testing.ui.data.SleepData
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.utils.Utils
import com.example.testing.utils.Utils.Companion.checkAccessibilityPermission
import com.example.testing.utils.Utils.Companion.countAvgSpeed
import com.example.testing.utils.Utils.Companion.readSharedSettingBoolean
import com.example.testing.utils.Utils.Companion.readSharedSettingString
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.round
import kotlin.math.roundToInt


// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(R.layout.fragment_home), OnSharedPreferenceChangeListener {
    // Chart variables:
    private val MAX_X_VALUE = 13
    private val GROUPS = 2
    private val GROUP_1_LABEL = "Orders"
    private val GROUP_2_LABEL = ""
    private val BAR_SPACE = 0.1f
    private val BAR_WIDTH = 0.8f
    private var lineChart: LineChart? = null
    private var chart: BarChart? = null
    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null
    private val statValues: ArrayList<Float> = ArrayList()
    protected val statsTitles = arrayOf(
        "Orders", "Inventory"
    )
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentDate = ""
    private val formatter = SimpleDateFormat("yyyy-MM-dd")
    var dateFragment = DateFragment()
    private val dateViewModel: DateViewModel by activityViewModels()
    private val firebaseViewModel: FirebaseViewModel by activityViewModels()
    var data = SleepData(false, 0, "", "")

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (key == "access_token") {
                checkFitbitLogin()
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


        if (sharedPrefs.contains("state") && sharedPrefs.contains("authorization_code")) {
            Log.d("HomeScreen", "Code and state not empty")
            val code = readSharedSettingString(
                Graph.appContext,
                "authorization_code", "")
            Log.d("sharedprefs", code.toString())
            val state = readSharedSettingString(
                Graph.appContext, "state", "")
            lifecycleScope.launch(Dispatchers.IO) {
                FitbitApiService.authorizeRequestToken(code!!, state!!)
            }
        } else {
            showFitbitLogin()
        }



        binding.keyboardChart.openAccessibilitySettingsBtn.setOnClickListener {
            checkAccessibilityPermission(Graph.appContext, true)
        }

        firebaseViewModel.keyboardData.observe(viewLifecycleOwner) {
            Log.d("FirebaseDebug", "Changes in firebase data")
            setFirebaseDataToUI()
        }

        binding.sleepDataContainer.FitbitBtn.setOnClickListener {
            val intent = Intent(activity, AuthenticationActivity::class.java)
            startActivity(intent)
        }

        dateViewModel.selectedDate.observe(viewLifecycleOwner) {
            Log.d("Dateviewmodel", "Date changed to: " +
            dateViewModel.selectedDate.value)
            //firebaseViewModel.clearListOfFirebaseData()
            updateKeyboardData()
            checkFitbitLogin()
            currentDate = dateViewModel.selectedDate.value.toString()
        }

        firebaseViewModel.sleepData.observe(viewLifecycleOwner) {
            Log.d("FirebaseViewModel","Sleep data updated")
            hideFitbitLogin(firebaseViewModel.sleepData.value!!)
        }

        //sharedPrefs.registerOnSharedPreferenceChangeListener(this)

    }

    private fun checkFitbitLogin() {
        if (!isLoggedInFitbit()) {
            showFitbitLogin()
            Log.d("Fitbit", "Not logged into fitbit")
        } else {
            Log.d("Fitbit", "Logged into fitbit")
            updateSleepData()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isLoggedInFitbit()) {
            showFitbitLogin()
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
        Log.d("GetSleepData", "Sleep data requested")
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
                Log.d("HomeFragment", "Sleep data found")
                println(data.endTime)
                println(data.startTime)
                wakeUpTime.text = data.endTime.toString()
                bedTime.text = data.startTime.toString()
                val t: Int = data.totalMinutesAsleep
                val hours = t / 60
                val minutes = t % 60
                val asleepString = "$hours h $minutes m"
                println(asleepString)
                sleepAmount.text = asleepString
                val participantId = readSharedSettingString(
                    Graph.appContext,
                    "p_id",
                    "")
                firebaseViewModel.saveSleepDataToFirebase(
                    dateViewModel.selectedDate.value.toString(),
                    data,
                    participantId.toString()
                )
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
        return if (!Utils.readSharedSettingBoolean(Graph.appContext,
                getString(R.string.sleep_data_key), true)) {
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
        Log.d("FirebaseDebug2", "KeyboardData: ${firebaseViewModel.keyboardData.value}")
        if (firebaseViewModel.keyboardData.value!!.isNotEmpty()) {
            val totalErr = mutableListOf<Double>()
            val totalSpeed = mutableListOf<Double>()
            val totalErrRate = mutableListOf<Double>()
            for (i in firebaseViewModel.keyboardData.value!!) {
                if (i.date == dateViewModel.selectedDate.value) {
                    totalErr.add(i.errors)
                    totalSpeed.add(i.speed)
                    totalErrRate.add(i.errorRate)
                }
            }
            Log.d("HomeFragment", "Total speed: ${totalSpeed.average()}l")
            Log.d("HomeFragment", "Total speed: ${totalErr.average()}l")
            Log.d("HomeFragment", "Total speed: ${totalErrRate.average()}l")
            binding.keyboardChart.keyboardDataNotFound.isVisible = false
            binding.keyboardChart.dataAvailable.isVisible = true
            val s = (60 / totalSpeed.average()).toString() //words per minute
            val clippedString = s.substring(0, s.length.coerceAtMost(4))
            binding.keyboardChart.speedData.text = clippedString
            binding.keyboardChart.ProgressTextView.text =
                showPercentage(totalErrRate.average(),
                    binding.keyboardChart.progressCircular).toString() + "%"
        } else {
            /**
        binding.keyboardChart.speedData.text = "No data"
        binding.keyboardChart.textViewStats.isVisible = false
        binding.keyboardChart.ProgressTextView.text = "--"
        binding.keyboardChart.progressCircular.isVisible = false**/
            binding.keyboardChart.keyboardDataNotFound.isVisible = true
            binding.keyboardChart.dataAvailable.isVisible = false
            Log.d("UpdateUI", "No data on keyboardList")
        }
    }

    fun showPercentage(errorRate: Double, progressBar: ProgressBar): Int {
        var successRate = (1.0 - errorRate) * 100
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