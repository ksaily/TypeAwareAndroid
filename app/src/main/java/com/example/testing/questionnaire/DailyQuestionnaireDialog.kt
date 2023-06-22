package com.example.testing.questionnaire

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.databinding.DialogDailyQuestionnaireBinding
import com.example.testing.utils.Utils
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DailyQuestionnaireDialog : DialogFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var _binding: DialogDailyQuestionnaireBinding? = null
    private val binding get() = _binding!!

    private var currentDate: String = Utils.currentDate
    private val myRef = Firebase.database.getReference("KeyboardEvents")
    private val participantId = Utils.readSharedSettingString(Graph.appContext,
        "p_id", "").toString()
    private val questionnaireCompleteString = "QuestionnaireCompleted"
    //private val viewModel: DailyQuestionnaireViewModel by activityViewModels()


    private var questions = listOf(
        "Q1: How much did you have to correct your typing yesterday compared to an average day?",
        "Q2: How fast was your typing speed yesterday compared to an average day?",
        "Q3: How many words did you type during yesterday?",
        "Q4: At what hour of the day were you typing the most yesterday? Choose the starting hour.",
        "Q5: Compared to an average day, how did you sleep yesterday?",
        "Q6: Did you go to bed earlier or later yesterday compared to an average day?",
    )
    private var currentQuestionIndex = 0
    private var currentAnswer = ""
    private var isFirstWeek = true

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
                showNextQuestion()
                setupClickListener()
            }
            Utils.getSharedPrefs().registerOnSharedPreferenceChangeListener(this)
        }
    }

    override fun onSharedPreferenceChanged(sharedPref: SharedPreferences?, key: String?) {
        if (key == getString(R.string.sharedpref_firstweek_done) &&
                Utils.readSharedSettingBoolean(Graph.appContext,
                getString(R.string.sharedpref_firstweek_done), false)) {
            changeToSecondWeek()
        }
    }

    private fun changeToSecondWeek() {
        questions = listOf(
            "Q1: Yesterday reflected a normal day for me.",
            "Q2: I found yesterday’s data meaningful.",
            "Q3: Did you find anything surprising in yesterday’s data?",
            "Q4: Based on yesterday’s data, I would be likely to change my digital or sleep-related behaviour."
        )
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
        numberPickerVisible: Boolean,
        secondWeekWordAnswerVisible: Boolean) {
        binding.apply {
            radioGroup1.isVisible = radioGroupVisible
            amountOfWordsAnswer.isVisible = wordAnswerVisible
            numberPicker.isVisible = numberPickerVisible
            week2Q2.isVisible = secondWeekWordAnswerVisible

        }
    }

    private fun loopForLikertScaleQuestions(arrayId: Int) {
        val radioGroup = binding.radioGroup1
        chooseViewsToShow(true, false, false, false)
        for (rbPosition in 0 until radioGroup.childCount) {
            val rb = radioGroup.getChildAt(rbPosition) as RadioButton
            rb.text = resources.getStringArray(
                arrayId)[rbPosition].toString()
        }
    }

    private fun setupView() {
        binding.questionTextView.text = questions[currentQuestionIndex]
        val rgGroup1 = binding.radioGroup1
        if (isFirstWeek) {
            when (currentQuestionIndex) {
                0 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options1)
                1 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options2)
                2 -> chooseViewsToShow(false, true, false, false)
                3 -> chooseViewsToShow(false, false, true, false)
                4 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options2)
                5 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options3)
            }
        } else {
            when (currentQuestionIndex) {
                0 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
                1 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
                2 -> chooseViewsToShow(false, false, false, true)
                3 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
            }
        }
    }



    private fun setupClickListener() {
        binding.submitButton.setOnClickListener {
            if (isFirstWeek) {
                when (currentQuestionIndex) {
                    (0 or 1 or 4 or 5) -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId).toString()
                    }
                    2 -> currentAnswer = binding.amountOfWordsAnswer.text.toString()

                    3 -> currentAnswer = binding.numberPicker.value.toString()
                }
                saveAnswerToDatabase(currentQuestionIndex, currentAnswer)
                currentQuestionIndex++
                showNextQuestion()
            } else {
                when (currentQuestionIndex) {
                    (0 or 1 or 3) -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId).toString()
                    }
                    2 -> {
                        val selectedRadioButtonId = binding.radioGroup2.checkedRadioButtonId
                        val ans = getSelectedOptionIndex(selectedRadioButtonId)
                        val textAns = binding.explainAnswer.text
                        currentAnswer = "$ans, $textAns"
                    }
                }
                saveAnswerToDatabase(currentQuestionIndex, currentAnswer)
                currentQuestionIndex++
                showNextQuestion()
            }
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

    fun showQuestionnaire(isFirstDay: Boolean) {
        val today = Utils.currentDate
        Log.d("Today:", today)
        var isQuestionnaireAnswered: Boolean

        if (!isFirstDay) {

            lifecycleScope.launch(Dispatchers.IO) {

                myRef.child(participantId).child(today).child("questionnaire")
                    .child(questionnaireCompleteString).get().addOnSuccessListener { snapshot ->
                        Log.d("Firebase", "Questionnaire listener")
                        isQuestionnaireAnswered = (snapshot.exists() &&
                                snapshot.value as Boolean)
                        Log.d("isQuestionnaireAnswered",
                            isQuestionnaireAnswered.toString())// both need to be true

                        Utils.saveSharedSettingBoolean(Graph.appContext,
                            "isQuestionnaireAnswered", isQuestionnaireAnswered)
                    }.addOnFailureListener {
                        // Error occurred while checking if questionnaire is answered, show home screen
                        showHomeScreen()
                    }
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
            }
            showHomeScreen()
        }
    }

    private fun saveAnswerToDatabase(question: Int, answer: String) {
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