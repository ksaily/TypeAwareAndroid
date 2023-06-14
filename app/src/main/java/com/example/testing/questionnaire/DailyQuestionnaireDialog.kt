package com.example.testing.questionnaire

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
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
        "Q2: How was your typing speed yesterday compared to an average day?",
        "Q3: How many words did you type during yesterday?",
        "Q4: At what time of day were you typing the most yesterday?",
        "Q5: Compared to an average day, how did you sleep yesterday?",
        "Q6: How much fatigue did you experience yesterday compared to an average day?",
    )
    private var currentQuestionIndex = 0

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
            showQuestionnaire()
            if (!Utils.readSharedSettingBoolean(Graph.appContext,
                "isQuestionnaireAnswered", false)) {
                setupView()
                setupClickListener()
            }
        }
    }

    private fun setupView() {
        binding.questionTextView.text = questions[currentQuestionIndex]
    }

    private fun setupClickListener() {
        binding.submitButton.setOnClickListener {
            val selectedRadioButtonId = binding.radioGroup1.checkedRadioButtonId
            val selectedOptionIndex = getSelectedOptionIndex(selectedRadioButtonId)
            saveAnswerToDatabase(currentQuestionIndex, selectedOptionIndex)
            currentQuestionIndex++
            showNextQuestion()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
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
        var isQuestionnaireAnswered: Boolean

        lifecycleScope.launch(Dispatchers.IO) {

            myRef.child(participantId).child(today).child("questionnaire")
                .child(questionnaireCompleteString).get().addOnSuccessListener { snapshot ->
                    Log.d("Firebase", "Questionnaire listener")
                    isQuestionnaireAnswered = (snapshot.exists() &&
                            snapshot.value as Boolean) // both need to be true
                    if (!isQuestionnaireAnswered) {
                        Utils.saveSharedSettingBoolean(Graph.appContext,
                        "isQuestionnaireAnswered", false)
                    } else {
                        Utils.saveSharedSettingBoolean(Graph.appContext,
                            "isQuestionnaireAnswered", true)
                    }
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
            val ans = "A$answer"
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