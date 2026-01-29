package com.example.finwise

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.CreateGoalRequest
import com.example.finwise.api.DepositRequest
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.SavingsGoalResponse
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavingsGoalsActivity : AppCompatActivity() {

    private lateinit var rvGoals: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var adapter: SavingsGoalAdapter
    private var goals: List<SavingsGoalResponse> = emptyList()
    
    private val userEmail: String by lazy {
        getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("user_email", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_goals)

        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Initialize views
        rvGoals = findViewById(R.id.rvGoals)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        fabAddGoal = findViewById(R.id.fabAddGoal)

        // Setup RecyclerView
        adapter = SavingsGoalAdapter(goals) { goal ->
            showDepositDialog(goal)
        }
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = adapter

        // FAB click listener
        fabAddGoal.setOnClickListener {
            showAddGoalDialog()
        }

        // Load goals
        loadGoals()
    }

    private fun loadGoals() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetchedGoals = RetrofitClient.instance.getSavingsGoals(userEmail)
                
                withContext(Dispatchers.Main) {
                    goals = fetchedGoals
                    adapter.updateGoals(goals)
                    
                    // Show/hide empty state
                    if (goals.isEmpty()) {
                        emptyStateLayout.visibility = View.VISIBLE
                        rvGoals.visibility = View.GONE
                    } else {
                        emptyStateLayout.visibility = View.GONE
                        rvGoals.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SavingsGoalsActivity, "Failed to load goals", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddGoalDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_goal, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etGoalTitle)
        val etTargetAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDeadline)

        AlertDialog.Builder(this)
            .setTitle("Create New Goal")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val title = etTitle.text.toString()
                val targetAmountStr = etTargetAmount.text.toString()
                val deadline = etDeadline.text.toString().ifBlank { null }

                if (title.isBlank() || targetAmountStr.isBlank()) {
                    Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val targetAmount = targetAmountStr.toFloatOrNull()
                if (targetAmount == null || targetAmount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createGoal(title, targetAmount, deadline)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createGoal(title: String, targetAmount: Float, deadline: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = CreateGoalRequest(
                    email = userEmail,
                    title = title,
                    target_amount = targetAmount,
                    deadline = deadline
                )
                
                RetrofitClient.instance.createGoal(request)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SavingsGoalsActivity, "Goal created successfully!", Toast.LENGTH_SHORT).show()
                    loadGoals()  // Refresh list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SavingsGoalsActivity, "Failed to create goal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDepositDialog(goal: SavingsGoalResponse) {
        val input = EditText(this)
        input.hint = "Enter amount to deposit"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        AlertDialog.Builder(this)
            .setTitle("Deposit to ${goal.title}")
            .setMessage("Current: ₹${String.format("%,.0f", goal.current_amount)} / ₹${String.format("%,.0f", goal.target_amount)}")
            .setView(input)
            .setPositiveButton("Deposit") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toFloatOrNull()
                
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                depositToGoal(goal.id, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun depositToGoal(goalId: Int, amount: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = DepositRequest(amount)
                val updatedGoal = RetrofitClient.instance.depositToGoal(goalId, request)
                
                withContext(Dispatchers.Main) {
                    if (updatedGoal.progress_percent >= 100) {
                        Toast.makeText(this@SavingsGoalsActivity, "🎉 Goal Reached!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@SavingsGoalsActivity, "Deposited ₹${String.format("%,.0f", amount)}", Toast.LENGTH_SHORT).show()
                    }
                    loadGoals()  // Refresh list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SavingsGoalsActivity, "Failed to deposit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
