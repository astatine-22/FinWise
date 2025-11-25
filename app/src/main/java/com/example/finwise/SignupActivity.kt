package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.SignupRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Inputs
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // Buttons
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // SIGNUP BUTTON ACTION
        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // 1. Basic Validation
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Call the API (Python)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = SignupRequest(name, email, pass)
                    val response = RetrofitClient.instance.signup(request)

                    withContext(Dispatchers.Main) {
                        // Success!
                        Toast.makeText(this@SignupActivity, "Account Created!", Toast.LENGTH_SHORT).show()

                        // Go to Home
                        val intent = Intent(this@SignupActivity, MainActivity::class.java)

                        // --- THIS WAS MISSING! ---
                        // Pass the email to the dashboard so it knows who we are
                        intent.putExtra("USER_EMAIL", email)
                        // -------------------------

                        // Clear back stack
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SignupActivity, "Signup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Login Link Action
        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Don't finish() so back button works
        }
    }
}