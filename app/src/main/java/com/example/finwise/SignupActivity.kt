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
import com.example.finwise.api.SessionManager
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
    private lateinit var sessionManager: SessionManager

    // Google Sign-In launcher
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this)

        // Initialize RetrofitClient
        RetrofitClient.initialize(this)

        // Configure Google Sign-In
        val gsoClientId = getString(R.string.google_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(gsoClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

    /**
     * Perform standard email/password signup.
     * On success, shows message and automatically logs in the user.
     */
    private fun performStandardSignup(name: String, email: String, pass: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val signupRequest = SignupRequest(name, email, pass)
                val signupResponse = RetrofitClient.instance.signup(signupRequest)
                
                // Signup successful, now auto-login to get JWT token
                val loginRequest = com.example.finwise.api.LoginRequest(email, pass)
                val loginResponse = RetrofitClient.instance.login(loginRequest)
                
                withContext(Dispatchers.Main) {
                    // Save JWT token and user data
                    saveSessionAndProceed(
                        token = loginResponse.access_token,
                        email = email,
                        name = loginResponse.name ?: name
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SignupActivity, "Signup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Sign in with Google
     */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /**
     * Handle Google authentication with backend.
     */
    private fun handleGoogleAuthInBackend(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.example.finwise.api.GoogleLoginRequest(idToken)
                val response = RetrofitClient.instance.googleLogin(request)

                withContext(Dispatchers.Main) {
                    val acct = GoogleSignIn.getLastSignedInAccount(this@SignupActivity)
                    val finalEmail = acct?.email ?: ""

                    if (finalEmail.isNotEmpty()) {
                        saveSessionAndProceed(
                            token = response.access_token,
                            email = finalEmail,
                            name = response.user?.name ?: acct?.displayName
                        )
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

    /**
     * Save session data and navigate to MainActivity.
     */
    private fun saveSessionAndProceed(token: String, email: String, name: String?) {
        // Save to SessionManager
        sessionManager.saveSession(token, email, name ?: "User")
        
        // Also save to legacy SharedPreferences for backward compatibility
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("LOGGED_IN_EMAIL", email)
            .putBoolean("COMPLETED_ONBOARDING", true)
            .apply()
        
        // Refresh RetrofitClient to pick up the new token
        RetrofitClient.refreshClient()

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