package com.example.testing.ui.menu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.testing.R
import com.example.testing.databinding.FragmentDateBinding
import com.example.testing.ui.viewmodel.DateViewModel
import com.example.testing.utils.Utils.Companion.getFromFirebase

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DateFragment : Fragment(R.layout.fragment_date) {

    private var _binding: FragmentDateBinding? = null
    private val binding get() = _binding!!
    //Initialize the viewmodel
    private val viewModel: DateViewModel by viewModels()

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
            binding.currentDate.text = viewModel.checkDate()
            //Get data from Firebase
            getFromFirebase(viewModel.selectedDate.value.toString())
            //Set up LiveData listener in Home and chart fragments:
            //Changes in selectedDate -> Update UI
        }

        binding.arrowRight.setOnClickListener {
            viewModel.nextDay()
            binding.currentDate.text = viewModel.checkDate()
            getFromFirebase(viewModel.selectedDate.value.toString())
        }

        viewModel.isToday.observe(viewLifecycleOwner
        ) { // Check if date is today, if yes, remove right arrow
            binding.arrowRight.isVisible = viewModel.isToday.value != true
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d("DateFragment", "DateFragment destroyed!")
    }

}