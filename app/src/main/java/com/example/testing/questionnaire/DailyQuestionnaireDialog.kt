package com.example.testing.questionnaire

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.provider.MediaStore.Audio.Radio
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RadioButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.DialogDailyQuestionnaireBinding
import com.example.testing.databinding.FragmentHomeBinding
import com.example.testing.ui.viewmodel.DailyQuestionnaireViewModel
import com.example.testing.utils.Utils
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DailyQuestionnaireDialog : DialogFragment() {

    private var _binding: DialogDailyQuestionnaireBinding? = null
    private val binding get() = _binding!!

    private var currentDate: String = Utils.currentDate
    private val myRef = Firebase.database.getReference("KeyboardEvents")
    private val participantId = Utils.readSharedSettingString(Graph.appContext,
        "p_id", "").toString()
    private val questionnaireCompleteString = "QuestionnaireCompleted"
    //private val viewModel: DailyQuestionnaireViewModel by activityViewModels()


    private val questions = listOf(
        "Q1: How much did you have to correct your typing yesterday compared to an average day?",
        "Q2: How fast was your typing speed yesterday compared to an average day?",
        "Q3: How many words did you type during yesterday?",
        "Q4: At what hour of the day were you typing the most yesterday? Choose the starting hour.",
        "Q5: Compared to an average day, how did you sleep yesterday?",
        "Q6: How much fatigue did you experience yesterday compared to an average day?",
    )
    private var currentQuestionIndex = 0
    private var currentAnswer = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogDailyQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            if (!Utils.readSharedSettingBoolean(Graph.appContext,
                "isQuestionnaireAnswered", false)) {
                setUpNumberPicker()
                setupView()
                setupClickListener()
            }
        }
    }

    private fun setUpNumberPicker() {
        val data = resources.getStringArray(R.array.questionnaire_time_of_day_options)
        binding.numberPicker.minValue = 0
        binding.numberPicker.maxValue = data.size-1
        binding.numberPicker.displayedValues = data
    }

    private fun chooseViewsToShow(
        radioGroupVisible: Boolean,
        wordAnswerVisible: Boolean,
        numberPickerVisible: Boolean) {
        binding.apply {
            radioGroup1.isVisible = radioGroupVisible
            amountOfWordsAnswer.isVisible = wordAnswerVisible
            numberPicker.isVisible = numberPickerVisible
        }
    }

    private fun setupView() {
        binding.questionTextView.text = questions[currentQuestionIndex]
        val rgGroup = binding.radioGroup1
        when (currentQuestionIndex) {
            2 -> {
                chooseViewsToShow(false, true, false)
            }
            3 -> {
                rgGroup.isVisible = false
                binding.amountOfWordsAnswer.isVisible = false
                binding.numberPicker.isVisible = true
                chooseViewsToShow(false, false, true)
            }
            else -> {
                chooseViewsToShow(true, false, false)
                for (rbPosition in 0 until rgGroup.childCount) {
                    val rb = rgGroup.getChildAt(rbPosition) as RadioButton
                    when (currentQuestionIndex) {

                        0 -> {
                            rb.text =
                                resources.getStringArray(
                                    R.array.questionnaire_likert_scale_options1)[rbPosition].toString()

                        }
                        (1 or 4) -> {
                            rb.text =
                                resources.getStringArray(
                                    R.array.questionnaire_likert_scale_options2)[rbPosition].toString()
                        }
                        5 -> {
                            rb.text =
                                resources.getStringArray(
                                    R.array.questionnaire_likert_scale_options3)[rbPosition].toString()
                        }
                    }

                }
            }
        }
    }



    private fun setupClickListener() {
        binding.submitButton.setOnClickListener {
            when (currentQuestionIndex) {
                (0 or 1 or 4 or 5) -> {
                    val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                    currentAnswer = getSelectedOptionIndex(selectedRadioButtonId)
                }
                2 -> currentAnswer = binding.amountOfWordsAnswer.text.toString().toInt()

                3 -> currentAnswer = binding.numberPicker.value
            }
            saveAnswerToDatabase(currentQuestionIndex, currentAnswer)
            currentQuestionIndex++
            showNextQuestion()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog?.setCanceledOnTouchOutside(false)
    }

/**
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
         // Use the Builder class for convenient dialog construction
        Log.d("AlertDialog", "onCreateDialog")
            _binding = DialogDailyQuestionnaireBinding.inflate(layoutInflater).apply {
                binding.questionTextView.text = questions[currentQuestionIndex]
            }
            val builder = AlertDialog.Builder(context)

            return builder
                .setView(binding.root)
                .setPositiveButton("Submit") { dialog, _ ->
                        val selectedRadioButtonId = binding.radioGroup.checkedRadioButtonId
                        val selectedOptionIndex = getSelectedOptionIndex(selectedRadioButtonId)

                        saveAnswerToDatabase(selectedOptionIndex)
                        currentQuestionIndex++
                        showNextQuestion()

                    }.create()

            // Create the AlertDialog object and return it
    }**/


    private fun getSelectedOptionIndex(selectedRadioButtonId: Int): Int {
        return when (selectedRadioButtonId) {
            R.id.likertScale1 -> 0
            R.id.likertScale2 -> 1
            R.id.likertScale3 -> 2
            R.id.likertScale4 -> 3
            R.id.likertScale5 -> 4
            R.id.likertScale6 -> 5
            R.id.likertScale7 -> 6
            else -> -1
        }
    }

    fun showQuestionnaire() {
        val today = Utils.currentDate
        Log.d("Today:", today)
        var isQuestionnaireAnswered: Boolean

        lifecycleScope.launch(Dispatchers.IO) {

            myRef.child(participantId).child(today).child("questionnaire")
                .child(questionnaireCompleteString).get().addOnSuccessListener { snapshot ->
                    Log.d("Firebase", "Questionnaire listener")
                    isQuestionnaireAnswered = (snapshot.exists() &&
                            snapshot.value as Boolean)
                    Log.d("isQuestionnaireAnswered", isQuestionnaireAnswered.toString())// both need to be true

                    Utils.saveSharedSettingBoolean(Graph.appContext,
                        "isQuestionnaireAnswered", isQuestionnaireAnswered)
                    }.addOnFailureListener {
                        // Error occurred while checking if questionnaire is answered, show home screen
                        showHomeScreen()
                    }
                }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            setupView()
        }
        else {
            lifecycleScope.launch(Dispatchers.IO) {
                val today = Utils.currentDate
                val questionnaireRef = myRef.child(participantId).child(today).child("questionnaire")

                questionnaireRef.child(questionnaireCompleteString).setValue(true).await()
                Utils.saveSharedSettingBoolean(Graph.appContext,
                    "isQuestionnaireAnswered", true)

                withContext(Dispatchers.Main) {
                    showHomeScreen()
                }
            }
        }
    }

    private fun saveAnswerToDatabase(question: Int, answer: Int) {
        lifecycleScope.launch(Dispatchers.IO) {

            val qstn = "Q$question"
            // Save data under the current timeslot with an unique id for each
            myRef.child(participantId)
                .child(currentDate).child("questionnaire").child(qstn)
                .setValue(answer)


                // Save the answered date to indicate the questionnaire has been answered for today
                //myRef.child(currentDate).setValue(true)

        }
    }

    private fun showHomeScreen() {
        if (dialog != null) {
            dismiss()
        }
        _binding = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val TAG = "DailyQuestionnaireDialog"

    }
}