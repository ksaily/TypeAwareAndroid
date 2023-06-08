package com.example.testing.questionnaire

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.testing.Graph
import com.example.testing.R
import com.example.testing.utils.Utils
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DailyQuestionnaireDialog : DialogFragment() {

    private lateinit var questionTextView: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var submitButton: Button

    private val database = FirebaseDatabase.getInstance()
    private val answeredDatesRef = database.getReference("answered_dates")
    private var currentDate: String = ""

    private val questions = listOf(
        "Q1: How often do you experience X?",
        "Q2: How often do you experience Y?",
        "Q3: How often do you experience Z?",
        "Q4: How often do you experience W?"
    )
    private var currentQuestionIndex = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var selectedItem: Int = 0
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(questions[currentQuestionIndex])
                .setSingleChoiceItems(R.array.questionnaire_likert_scale_options, 0,
                    DialogInterface.OnClickListener { dialog, which ->
                        // The 'which' argument contains the index position
                        // of the selected item
                        selectedItem = which

                    })
                .setPositiveButton("Submit",
                    DialogInterface.OnClickListener { dialog, id ->
                        saveAnswerToDatabase(selectedItem)
                        currentQuestionIndex++
                        if (currentQuestionIndex < questions.size) {
                            showNextQuestion()
                        } else {
                            dialog.dismiss()
                            showHomeScreen()
                        }

                    })

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw java.lang.IllegalStateException("Activity cannot be null")
    }

    fun showQuestionnaire() {
        val today = Utils.currentDate

        answeredDatesRef.child(today).get().addOnSuccessListener { snapshot ->
            val isQuestionnaireAnswered = snapshot.exists()

            if (isQuestionnaireAnswered) {
                // Questionnaire already answered for today, show home screen
                showHomeScreen()
            } else {
                // Questionnaire not answered for today, show the dialog
                showNextQuestion()
            }
        }.addOnFailureListener {
            // Error occurred while checking if questionnaire is answered, show home screen
            showHomeScreen()
        }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            /**dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.dialog_daily_questionnaire)

            questionTextView = dialog.findViewById(R.id.questionTextView)
            ratingBar = dialog.findViewById(R.id.ratingBar)
            submitButton = dialog.findViewById(R.id.submitButton)
**/
            questionTextView.text = questions[currentQuestionIndex]

            submitButton.setOnClickListener {
                val answer = ratingBar.rating.toInt()
                saveAnswerToDatabase(answer)

                currentQuestionIndex++
                if (currentQuestionIndex < questions.size) {
                    showNextQuestion()
                } else {
                    dialog!!.dismiss()
                    showHomeScreen()
                }
            }

            dialog!!.show()
        } else {
            // All questions answered, show home screen
            showHomeScreen()
        }
    }

    private fun saveAnswerToDatabase(answer: Int) {
        CoroutineScope(Dispatchers.IO).launch {

                val userAnswersRef = database.getReference("answers").child("user_id").push()
                userAnswersRef.setValue(answer)

                // Save the answered date to indicate the questionnaire has been answered for today
                answeredDatesRef.child(currentDate).setValue(true)

        }
    }

    private fun showHomeScreen() {
        // Show the app's home screen
    }
}