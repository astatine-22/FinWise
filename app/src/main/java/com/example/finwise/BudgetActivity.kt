package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetActivity : AppCompatActivity() {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvExpenses: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnBack: ImageView

    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // 1. Initialize Views
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        tvRemaining = findViewById(R.id.tvRemaining)
        progressBar = findViewById(R.id.progressBarBudget)
        rvExpenses = findViewById(R.id.rvExpenses)
        fabAdd = findViewById(R.id.fabAdd)
        btnBack = findViewById(R.id.btnBack)

        // 2. Setup List
        rvExpenses.layoutManager = LinearLayoutManager(this)

        // 3. Get Email
        userEmail = intent.getStringExtra("USER_EMAIL")

        // 4. Back Button
        btnBack.setOnClickListener {
            finish()
        }

        // 5. Add Expense Button
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail) // Pass email so we know who is adding
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when we come back to this screen
        if (userEmail != null) {
            loadBudgetData(userEmail!!)
        }
    }

    private fun loadBudgetData(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch Data from Python
                val expensesList = RetrofitClient.instance.getExpenses(email)
                val summary = RetrofitClient.instance.getBudgetSummary(email)

                withContext(Dispatchers.Main) {
                    // Update List
                    val adapter = ExpenseAdapter(expensesList)
                    rvExpenses.adapter = adapter

                    // Update Summary Card
                    tvTotalSpent.text = "₹${summary.total_spent.toInt()}"

                    val percentage = (summary.total_spent / summary.limit) * 100
                    progressBar.progress = percentage.toInt()

                    tvRemaining.text = "₹${summary.remaining.toInt()} left to spend"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // e.printStackTrace() // Uncomment to see error in Logcat
                }
            }
        }
    }
}