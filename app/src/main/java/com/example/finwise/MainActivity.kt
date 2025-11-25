package com.example.finwise

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Find the Views
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvXp = findViewById<TextView>(R.id.tvXp)
        val cardBudget = findViewById<CardView>(R.id.cardBudget) // The Budget Tracker Card

        // 2. Get the email passed from Login
        val userEmail = intent.getStringExtra("USER_EMAIL")

        if (userEmail != null) {
            // Fetch Name & XP from Python
            fetchUserData(userEmail, tvWelcome, tvXp)

            // 3. BUDGET CARD CLICK LISTENER
            cardBudget.setOnClickListener {
                val intent = Intent(this, BudgetActivity::class.java)
                intent.putExtra("USER_EMAIL", userEmail) // Pass email to Budget screen too
                startActivity(intent)
            }

        } else {
            tvWelcome.text = "Error: No Email Found"
        }
    }

    private fun fetchUserData(email: String, tvWelcome: TextView, tvXp: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Call Python API
                val userProfile = RetrofitClient.instance.getUserDetails(email)

                withContext(Dispatchers.Main) {
                    // Update Dashboard UI
                    tvWelcome.text = "Welcome, ${userProfile.name}!"
                    tvXp.text = userProfile.xp.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvWelcome.text = "Welcome, User (Offline)"
                    // Optional: Toast.makeText(this@MainActivity, "Sync Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}