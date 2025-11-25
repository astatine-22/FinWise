package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvXp = findViewById<TextView>(R.id.tvXp)
        val cardBudget = findViewById<CardView>(R.id.cardBudget)
        // Get reference to the bell icon
        val btnLogout = findViewById<ImageView>(R.id.btnNotificationBell)

        val userEmail = intent.getStringExtra("USER_EMAIL")

        if (userEmail != null) {
            fetchUserData(userEmail, tvWelcome, tvXp)

            cardBudget.setOnClickListener {
                val intent = Intent(this, BudgetActivity::class.java)
                intent.putExtra("USER_EMAIL", userEmail)
                startActivity(intent)
            }

            // --- LOGOUT LOGIC (NEW!) ---
            btnLogout.setOnClickListener {
                // 1. Open the notebook and erase the email
                val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()

                // 2. Go back to Login screen
                val intent = Intent(this, LoginActivity::class.java)
                // Clear the back stack so they can't press "back" to get into the app again
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            // ---------------------------

        } else {
            tvWelcome.text = "Error: No Email Found"
            // If no email, force logout immediately
            val sharedPref = getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun fetchUserData(email: String, tvWelcome: TextView, tvXp: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProfile = RetrofitClient.instance.getUserDetails(email)

                withContext(Dispatchers.Main) {
                    tvWelcome.text = "Welcome, ${userProfile.name}!"
                    tvXp.text = userProfile.xp.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvWelcome.text = "Welcome, User (Offline)"
                }
            }
        }
    }
}