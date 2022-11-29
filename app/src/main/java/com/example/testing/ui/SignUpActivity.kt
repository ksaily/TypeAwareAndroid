package com.example.testing.ui

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testing.R
import com.example.testing.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.signInHereBtn.setOnClickListener { //If the user already has an account
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.signUpBtn.setOnClickListener{
            val email = binding.createEmail.text.toString()
            val passwrd = binding.createPassword.text.toString()
            val confirmPass = binding.confirmPassword.text.toString()

            //Check that none of the fields are empty
            if (email.isNotEmpty() && passwrd.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (passwrd == confirmPass) { //Password and confirm password need to match
                    firebaseAuth.createUserWithEmailAndPassword(email, passwrd)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val intent = Intent(this, SignInActivity::class.java)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

}