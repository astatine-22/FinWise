package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.LoginRequest
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Views
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignup = findViewById<TextView>(R.id.tvSignupLink)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        // 1. LOGIN BUTTON ACTION
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Basic Validation
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call Python API
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = LoginRequest(email, pass)
                    // This calls your Python backend to check password
                    val response = RetrofitClient.instance.login(request)

                    withContext(Dispatchers.Main) {
                        // SUCCESS!
                        Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()

                        // Pass the email to the Home Screen so we can fetch their Name/XP there
                        navigateToHome(email)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // FAILURE (Wrong password or user not found)
                        Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 2. Google Button (Simulation)
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Login clicked", Toast.LENGTH_SHORT).show()
            // Simulating a login with a dummy email for now
            navigateToHome("alex@example.com")
        }

        // 3. Signup Link
        tvSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            // Note: We don't finish() here so user can press Back to return
        }
    }

    // Helper to switch screens and pass data
    private fun navigateToHome(email: String) {
        val intent = Intent(this, MainActivity::class.java)

        // PASS THE EMAIL to the next screen
        intent.putExtra("USER_EMAIL", email)

        // Clear the back stack (so Back button exits app instead of going to Login)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}