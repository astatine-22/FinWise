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
import com.example.finwise.api.LoginRequest
import com.example.finwise.api.RetrofitClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    // REMOVED: private val RC_SIGN_IN = 9001

    // --- NEW: Define the Activity Result Launcher ---
    // This replaces onActivityResult. It handles the response immediately.
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // This block runs when the user comes back from the Google account picker screen
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            // Google Sign In was successful, authenticate with Backend
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed
            Log.w("GoogleLogin", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In Failed. Code: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }
    // -------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Auto-login check ---
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)
        if (savedEmail != null) {
            goToMainActivity(savedEmail)
            return
        }
        // ------------------------

        setContentView(R.layout.activity_login)

        // --- CONFIGURE GOOGLE SIGN-IN ---
        val gsoClientId = getString(R.string.google_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(gsoClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // --------------------------------

        // Views
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignupLink = findViewById<TextView>(R.id.tvSignupLink)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        // --- NORMAL LOGIN BUTTON ---
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performStandardLogin(email, password, sharedPref)
        }

        // --- GOOGLE LOGIN BUTTON ---
        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Signup Link
        tvSignupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    // --- Helper functions for Standard Login ---
    private fun performStandardLogin(email: String, pass: String, sharedPref: android.content.SharedPreferences) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = LoginRequest(email, pass)
                RetrofitClient.instance.login(request)
                withContext(Dispatchers.Main) {
                    saveUserAndProceed(email, sharedPref)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- GOOGLE SIGN-IN FUNCTIONS ---

    private fun signInWithGoogle() {
        // UPDATED: Use the new launcher instead of startActivityForResult
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // REMOVED: override fun onActivityResult(...) { ... }

    private fun firebaseAuthWithGoogle(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.example.finwise.api.GoogleLoginRequest(idToken)
                val response = RetrofitClient.instance.googleLogin(request)

                withContext(Dispatchers.Main) {
                    val acct = GoogleSignIn.getLastSignedInAccount(this@LoginActivity)
                    val finalEmail = acct?.email ?: ""

                    if (finalEmail.isNotEmpty()) {
                        saveUserAndProceed(finalEmail, getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE))
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: Could not get email from Google", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("GoogleLogin", "Backend verification failed", e)
                    googleSignInClient.signOut()
                    Toast.makeText(this@LoginActivity, "Login Failed: Backend rejected token.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- COMMON NAVIGATION ---
    private fun saveUserAndProceed(email: String, sharedPref: android.content.SharedPreferences) {
        sharedPref.edit().putString("LOGGED_IN_EMAIL", email).putBoolean("COMPLETED_ONBOARDING", true).apply()
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
        goToMainActivity(email)
    }

    private fun goToMainActivity(email: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_EMAIL", email)
        startActivity(intent)
        finish()
    }
}