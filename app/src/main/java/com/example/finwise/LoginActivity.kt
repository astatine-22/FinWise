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
import com.example.finwise.api.SessionManager
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
    private lateinit var sessionManager: SessionManager

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            authenticateWithBackend(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("GoogleLogin", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In Failed. Code: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        sessionManager = SessionManager.getInstance(this)

        // Initialize RetrofitClient with context
        RetrofitClient.initialize(this)

        // --- Auto-login check using SessionManager ---
        if (sessionManager.isLoggedIn()) {
            val email = sessionManager.fetchUserEmail()
            if (email != null) {
                goToMainActivity(email)
                return
            }
        }
        // Also check legacy SharedPreferences for backward compatibility
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)
        if (savedEmail != null) {
            goToMainActivity(savedEmail)
            return
        }
        // ------------------------

        setContentView(R.layout.activity_login)

        // Configure Google Sign-In
        val gsoClientId = getString(R.string.google_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(gsoClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

            performStandardLogin(email, password)
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

    /**
     * Perform standard email/password login.
     * On success, saves JWT token and user info to SessionManager.
     */
    private fun performStandardLogin(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.instance.login(request)
                
                withContext(Dispatchers.Main) {
                    // Save JWT token and user data
                    saveSessionAndProceed(
                        token = response.access_token,
                        email = email,
                        name = response.user?.name
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Login Failed: ${e.message}", Toast.LENGTH_LONG).show()
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
     * Authenticate with backend using Google ID token.
     */
    private fun authenticateWithBackend(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = com.example.finwise.api.GoogleLoginRequest(idToken)
                val response = RetrofitClient.instance.googleLogin(request)

                withContext(Dispatchers.Main) {
                    val acct = GoogleSignIn.getLastSignedInAccount(this@LoginActivity)
                    val finalEmail = acct?.email ?: ""

                    if (finalEmail.isNotEmpty()) {
                        // Save JWT token and user data
                        saveSessionAndProceed(
                            token = response.access_token,
                            email = finalEmail,
                            name = response.user?.name ?: acct?.displayName
                        )
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

    /**
     * Save session data and navigate to MainActivity.
     * This is the central point for handling successful authentication.
     */
    private fun saveSessionAndProceed(token: String, email: String, name: String?) {
        // Save to SessionManager (new secure storage)
        sessionManager.saveSession(token, email, name ?: "User")
        
        // Also save to legacy SharedPreferences for backward compatibility AND offline fallback
        val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("LOGGED_IN_EMAIL", email)
            .putString("USER_NAME", name ?: "User")  // Save name for Profile offline fallback
            .putInt("USER_XP", 0)  // Default XP, will be updated by API later
            .putBoolean("COMPLETED_ONBOARDING", true)
            .apply()
        
        // Refresh RetrofitClient to pick up the new token
        RetrofitClient.refreshClient()
        
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