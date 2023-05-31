package com.example.testing.ui.menu

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.databinding.FragmentDateBinding
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.ui.viewmodel.FirebaseViewModel
import com.example.testing.utils.Utils.Companion.currentDate
import com.example.testing.utils.Utils.Companion.formatDateString
import com.example.testing.utils.Utils.Companion.formatter
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DateFragment : Fragment(R.layout.fragment_date) {

    private var _binding: FragmentDateBinding? = null
    private val binding get() = _binding!!
    //Initialize the viewmodel
    private val viewModel: DateViewModel by activityViewModels()
    private var date: String? = null

    companion object {
        var chosenDate: String = ""

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding.currentDate.text = "Today"
        //chosenDate = currentDate
        binding.arrowLeft.setOnClickListener {
            viewModel.previousDay()
            //date = viewModel.checkDate()
            binding.currentDate.text = viewModel.checkDate()
            //Get data from Firebase
            //getFromFirebase(viewModel.selectedDate.value.toString())
            //Set up LiveData listener in Home and chart fragments:
            //Changes in selectedDate -> Update UI
        }

        binding.arrowRight.setOnClickListener {
            viewModel.nextDay()
            binding.currentDate.text = viewModel.checkDate()
            //getFromFirebase(viewModel.selectedDate.value.toString())
        }

        binding.PickDate.setOnClickListener {
            val c = Calendar.getInstance()
            val currentDate = viewModel.selectedDate.value
            c.time = formatter.parse(currentDate) as Date

            // on below line we are getting
            // our day, month and year.
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // on below line we are creating a
            // variable for date picker dialog.
            val datePickerDialog = DatePickerDialog(
                // on below line we are passing context.
                this.activity!!.window!!.context,
                { view, year, monthOfYear, dayOfMonth ->
                    // on below line we are setting
                    // date to our text view.
                    val dateString = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                    //binding.currentDate.text = dateString
                    c.set(year, monthOfYear, dayOfMonth)
                    val newDate = c.time
                    val dateChosen = formatter.format(newDate)
                    viewModel.changeDate(dateChosen)
                    binding.currentDate.text = viewModel.checkDate()
                },
                // on below line we are passing year, month
                // and day for the selected date in our date picker.
                year,
                month,
                day
            )
            // at last we are calling show
            // to display our date picker dialog.
            datePickerDialog.show()
        }

        viewModel.isToday.observe(viewLifecycleOwner
        ) { // Check if date is today, if yes, remove right arrow
            binding.arrowRight.isVisible = viewModel.isToday.value != true
            binding.arrowRightText.isVisible = viewModel.isToday.value != true
        }
        viewModel.checkDate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDate()
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("DateFragment", "DateFragment destroyed!")
    }

}