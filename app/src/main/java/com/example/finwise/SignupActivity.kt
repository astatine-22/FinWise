package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.SignupRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    // REMOVED: private val RC_SIGN_IN = 9001

    // --- NEW: Define the Activity Result Launcher ---
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            handleGoogleAuthInBackend(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleSignup", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In Failed.", Toast.LENGTH_SHORT).show()
        }
    }
    // -------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // --- CONFIGURE GOOGLE SIGN-IN ---
        val gsoClientId = getString(R.string.google_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(gsoClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // --------------------------------

        // Views
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val btnGoogleSignup = findViewById<Button>(R.id.btnGoogleSignup)

        // --- NORMAL SIGNUP BUTTON ---
        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performStandardSignup(name, email, pass)
        }

        // --- GOOGLE SIGNUP BUTTON ---
        btnGoogleSignup.setOnClickListener {
            signInWithGoogle()
        }

        // Login Link
        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // --- Helper functions for Standard Signup ---
    private fun performStandardSignup(name: String, email: String, pass: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = SignupRequest(name, email, pass)
                RetrofitClient.instance.signup(request)
                withContext(Dispatchers.Main) {
                    saveUserAndProceed(email)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Signup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- GOOGLE SIGN-IN FUNCTIONS ---
    private fun signInWithGoogle() {
        // UPDATED: Use the new launcher
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // REMOVED: override fun onActivityResult(...) { ... }

    private fun handleGoogleAuthInBackend(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.example.finwise.api.GoogleLoginRequest(idToken)
                val response = RetrofitClient.instance.googleLogin(request)

                withContext(Dispatchers.Main) {
                    val acct = GoogleSignIn.getLastSignedInAccount(this@SignupActivity)
                    val finalEmail = acct?.email ?: ""

                    if (finalEmail.isNotEmpty()) {
                        saveUserAndProceed(finalEmail)
                    } else {
                        Toast.makeText(this@SignupActivity, "Error: Could not get email", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("GoogleSignup", "Backend verification failed", e)
                    googleSignInClient.signOut()
                    Toast.makeText(this@SignupActivity, "Signup Failed: Backend rejected token.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- COMMON NAVIGATION ---
    private fun saveUserAndProceed(email: String) {
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("LOGGED_IN_EMAIL", email)
            .putBoolean("COMPLETED_ONBOARDING", true)
            .apply()

        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
        goToMainActivity(email)
    }

    private fun goToMainActivity(email: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_EMAIL", email)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}