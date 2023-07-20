package com.example.testing.questionnaire

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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

/**
 * First week, show only survey for the user: How do you think you did this week
 * Second week, survey first and then reveal them the data
 * Q1: How many words did you type during date x?
 * Q2: Were you generally typing faster than normally
 * (Compare the participant's performance to others, how they feel about it)
 * Q2: How often did you have to correct your typing? (Scale 1-7)
 * Q3: At what time of day were you most active with typing? (some kind of selector)
 *
 */
class DailyQuestionnaireDialog : DialogFragment(){

    private var _binding: DialogDailyQuestionnaireBinding? = null
    private val binding get() = _binding!!

    private var currentDate: String = Utils.getCurrentDateString()
    private val myRef = Firebase.database.getReference("Data")
    private val participantId = Utils.readSharedSettingString("p_id", "").toString()
    private val questionnaireCompleteString = "QuestionnaireCompleted"
    //private val viewModel: DailyQuestionnaireViewModel by activityViewModels()


    private var questions = listOf(
        "Q1: How much did you have to correct your typing yesterday compared to an average day?",
        "Q2: How fast was your typing speed yesterday compared to an average day?",
        "Q3: How many words did you type during yesterday?",
        "Q4: At what hour of the day were you typing the most yesterday? Choose the starting hour.",
        "Q5: Compared to an average day, how did you sleep yesterday?",
        "Q6: Did you go to bed earlier or later yesterday compared to an average day?"
    )
    private var currentQuestionIndex = 0
    private var currentAnswer = ""
    private var isFirstWeek = true
    private var isFinished = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = DialogDailyQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity != null) {
            if (!Utils.readSharedSettingBoolean("isQuestionnaireAnswered", false)) {
                setUpNumberPicker()
                showNextQuestion()
                setupClickListener()
            }
        }
    }

    fun firstWeekQuestions() {
        questions = listOf(
            "Q1: How much did you have to correct your typing yesterday compared to an average day?",
            "Q2: How fast was your typing speed yesterday compared to an average day?",
            "Q3: How many words per minute did you type on average yesterday?",
            "Q4: At what hour of the day were you typing the most yesterday? Choose the starting hour.",
            "Q5: Compared to an average day, how did you sleep yesterday?",
            "Q6: Did you go to bed earlier or later yesterday compared to an average day?"
        )
    }

    fun changeToSecondWeek() {
        Log.d("Questionnaire", "Changed to second week")
        isFirstWeek = false
        isFinished = false
        secondWeekQuestions()
        Log.d("Questions:", "$questions")
    }

    private fun secondWeekQuestions() {
        questions = listOf(
            "Q1: Yesterday reflected a normal day for me.",
            "Q2: I found yesterday’s data meaningful.",
            "Q3: Did you find anything surprising in yesterday’s data?",
            "Q4: Based on yesterday’s data, I would be likely to change my digital or sleep-related behaviour."
        )
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft: FragmentTransaction = manager.beginTransaction()
            ft.add(this, tag)
            ft.commit()
        } catch (e: IllegalStateException) {
            Log.d("FragmentManager", "Exception", e)
        }
    }

    fun changeToEndQuestionnaire() {
        Log.d("Questionnaire", "Changed to end questionnaire")
        isFirstWeek = false
        isFinished = true
        endQuestions()
    }

    private fun endQuestions() {
        questions = listOf(
            "You have now completed two weeks of the study! Please finish the study by answering the following end questionnaire.",
            "Q1: Would you make any life or behavioral changes based on data like this? Why or why not?",
            "Q2: What type of changes would you make in your life based on data like this?",
            "Q3: Have you made some behavioral changes to accommodate your digital wellbeing prior to this study? How effective/ineffective were they?",
            "Q4: How suitable would this information be for other people or is it more useful just for you?",
            "You now have answered all the questionnaires and finished the study, congratulations! \n" +
                    "You can enter this code into Prolific to receive your compensation: \n" +
                    "If you wish to fill out an additional end questionnaire, you can find the link on the initial instructions that you received." +
                    "at the beginning of the study."
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
        secondWeekWordAnswerVisible: Boolean,
    ) {
        binding.apply {
            radioGroup1.isVisible = radioGroupVisible
            amountOfWordsAnswer.isVisible = wordAnswerVisible
            numberPicker.isVisible = numberPickerVisible
            week2Q2.isVisible = secondWeekWordAnswerVisible
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }
    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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
        rgGroup1.clearCheck()
        binding.radioGroup2.clearCheck()
        binding.openAnswer.text.clear()
        if (isFirstWeek) {
            firstWeekQuestions()
            chooseViewsToShow(true, false, false, false)
            when (currentQuestionIndex) {
                0 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options1)
                1 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options2)
                2 -> chooseViewsToShow(false, true, false, false)
                3 ->  {
                    hideKeyboard()
                    chooseViewsToShow(false, false, true, false)
                }
                4 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options2)
                5 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options3)
            }
        } else if (!isFinished) {
            secondWeekQuestions()
            chooseViewsToShow(true, false, false, false)
            when (currentQuestionIndex) {
                0 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
                1 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
                2 -> chooseViewsToShow(false, false, false, true)
                3 -> loopForLikertScaleQuestions(R.array.questionnaire_likert_scale_options_week2)
            }
        } else {
            endQuestions()
            binding.questionnaireTitle.text = "End questionnaire"
            when (currentQuestionIndex) {
                0 -> {
                    chooseViewsToShow(false, false, false, false)
                    binding.submitButton.text = "Continue"
                }
                1 -> {
                    chooseViewsToShow(false, false, false, true)
                    binding.submitButton.text = "Submit"
                }
                2 -> {
                    chooseViewsToShow(false, false, false, false)
                    binding.openAnswer.isVisible = true
                }
                3 -> {
                    chooseViewsToShow(false, false, false, true)
                    binding.openAnswer.isVisible = false
                }
                4 -> {
                    chooseViewsToShow(false, false, false, false)
                    binding.openAnswer.isVisible = true
                }
                5 -> {
                    binding.openAnswer.isVisible = false
                    binding.submitButton.text = "Finish"
                }
            }
        }
    }



    private fun setupClickListener() {
        binding.submitButton.setOnClickListener {
            if (isFirstWeek) {
                when (currentQuestionIndex) {
                    0 -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId).toString()
                    }
                    1 -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId).toString()
                    }
                    2 -> currentAnswer = binding.amountOfWordsAnswer.text.toString()

                    3 -> currentAnswer = binding.numberPicker.value.toString()
                    4 -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId)
                    }
                    5 -> {
                        val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId)
                    }
                }
                checkAnswerNotEmpty()
            } else if (!isFinished) {
                // If second week is of questionnaires
                when (currentQuestionIndex) {
                    0 -> { val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId) }
                    1 -> { val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId) }
                    2 -> {
                        val selectedRadioButtonId = binding.radioGroup2.checkedRadioButtonId
                        val textAns = binding.explainAnswer.text
                        currentAnswer = "$selectedRadioButtonId, $textAns"
                    }
                    3 -> { val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
                        currentAnswer = getSelectedOptionIndex(selectedRadioButtonId) }
                }
                checkAnswerNotEmpty()
            } else {
                when (currentQuestionIndex) {
                    0 -> { currentQuestionIndex++
                        showNextQuestion()}
                    1 -> {
                        val selectedRadioButtonId = binding.radioGroup2.checkedRadioButtonId
                        val textAns = binding.explainAnswer.text
                        currentAnswer = "$selectedRadioButtonId, $textAns"
                        //saveAnswerToDatabase(currentQuestionIndex+1, currentAnswer)
                        checkAnswerNotEmpty()
                    }
                    2 -> {
                        val ans = binding.openAnswer.text
                        currentAnswer = "$ans"
                        //saveAnswerToDatabase(currentQuestionIndex+1, currentAnswer)
                        checkAnswerNotEmpty()
                    }
                    3 -> {
                        val selectedRadioButtonId = binding.radioGroup2.checkedRadioButtonId
                        val textAns = binding.explainAnswer.text
                        currentAnswer = "$selectedRadioButtonId, $textAns"
                        //saveAnswerToDatabase(currentQuestionIndex+1, currentAnswer)
                        checkAnswerNotEmpty()
                    }
                    4 -> {
                        val ans = binding.openAnswer.text
                        currentAnswer = "$ans"
                        //saveAnswerToDatabase(currentQuestionIndex+1, currentAnswer)
                        checkAnswerNotEmpty()
                    }
                    5 -> {
                        currentQuestionIndex++
                        showNextQuestion()
                    }
                }

            }
            //currentQuestionIndex++
            //showNextQuestion()
        }
    }

    private fun checkAnswerNotEmpty() {
        if (currentAnswer.isNullOrEmpty() || currentAnswer == "-1") {
            Toast.makeText(Graph.appContext, "Please write or select an answer.", Toast.LENGTH_SHORT)
        } else {
            saveAnswerToDatabase(currentQuestionIndex+1, currentAnswer)
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


    private fun getSelectedOptionIndex(selectedRadioButtonId: Int): String {
        return when (selectedRadioButtonId) {
            R.id.likertScale1 -> "1"
            R.id.likertScale2 -> "2"
            R.id.likertScale3 -> "3"
            R.id.likertScale4 -> "4"
            R.id.likertScale5 -> "5"
            R.id.likertScale6 -> "6"
            R.id.likertScale7 -> "7"
            else -> "-"
        }
    }

    fun showQuestionnaire(isFirstDay: Boolean) {
        currentQuestionIndex = 0
        val today = Utils.getCurrentDateString()
        Log.d("ShowQuestionnaireToday:", today)
        var isQuestionnaireAnswered: Boolean
        Log.d("isFirstDay", isFirstDay.toString())

        if (!isFirstDay) {

            lifecycleScope.launch(Dispatchers.IO) {
                val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
                val questionnaire = "questionnaire"

                myRef.child(authId).child(participantId).child(today).child(questionnaire)
                    .child(questionnaireCompleteString).get().addOnSuccessListener { snapshot ->
                        Log.d("Firebase", "Questionnaire listener")
                        isQuestionnaireAnswered = (snapshot.exists() &&
                                snapshot.value as Boolean)
                        Log.d("isQuestionnaireAnswered",
                            isQuestionnaireAnswered.toString())// both need to be true

                        Utils.saveSharedSettingBoolean("isQuestionnaireAnswered", isQuestionnaireAnswered)
                    }.addOnFailureListener {
                        Log.d("DailyQuestionnaire", "error: $it")
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
            // Save info to Firebase and sharedPrefs on questionnaire completed
            lifecycleScope.launch(Dispatchers.IO) {
                val today = Utils.getCurrentDateString()
                val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
                val questionnaire = "questionnaire"

                val questionnaireRef = myRef.child(authId).child(participantId).child(today).child(questionnaire)

                questionnaireRef.child(questionnaireCompleteString).setValue(true).await()
                Utils.saveSharedSettingBoolean("isQuestionnaireAnswered", true)
                if (isFinished) {
                    Utils.saveSharedSettingBoolean("study_finished", true)
                    questionnaireRef.child("endQuestionnaireAnswered").setValue(true)
                }
            }
            //Check how many questionnaires have been answered
            val answered = Utils.readSharedSettingInt("number_of_questionnaires", 0)
            Log.d("Questionnaires answered", answered.toString())
            if (answered == null) {
                firstWeekQuestions()
                Log.d("QuestionnaireAnswerAmount", "Was 0, now 1")
                Utils.saveSharedSettingInt("number_of_questionnaires", 1)
            }
            else {
                val answeredNew = answered + 1
                Utils.saveSharedSettingInt("number_of_questionnaires", answeredNew)
                when (answered) {
                    in 5..10 -> {
                        Log.d("QuestionnaireAnswerAmount", "Was $answered, now $answeredNew")
                        Utils.saveSharedSettingBoolean("first_week_done", true)
                        showHomeScreen()
                    }
                    11 -> {
                        Log.d("End qstnr", "changed")
                        Utils.saveSharedSettingBoolean("study_finished", true)
                        showHomeScreen()
                    }
                    else -> {
                        showHomeScreen()
                    }
                }

            }
        }
    }

    private fun saveAnswerToDatabase(question: Int, answer: String) {
        lifecycleScope.launch(Dispatchers.IO) {

            val qstn = "Q$question"
            val authId = Utils.readSharedSettingString("firebase_auth_uid", "").toString()
            val questionnaire = "questionnaire"
            Log.d("Save answer to database", "$qstn $answer")
            // Save data under the current timeslot with an unique id for each
            myRef.child(authId).child(participantId)
                .child(Utils.getCurrentDateString()).child(questionnaire).child(qstn)
                .setValue(answer)
        }
    }

    private fun showHomeScreen() {
        if (dialog != null && dialog!!.isShowing) {
            dismiss()
        }
    }


    companion object {

        const val TAG = "DailyQuestionnaireDialog"

    }
}