package com.example.finwise

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.api.ExpenseCreateRequest
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExpenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etNote = findViewById<EditText>(R.id.etNote)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // 1. Get Email from intent
        val userEmail = intent.getStringExtra("USER_EMAIL")

        // 2. Setup Spinner (Dropdown)
        val categories = arrayOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        // 3. Back Button
        btnBack.setOnClickListener { finish() }

        // 4. Save Button Logic
        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()
            val note = etNote.text.toString().trim()
            val category = spinner.selectedItem.toString()

            // Validation
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (userEmail == null) {
                Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send to Python
            val amount = amountStr.toDouble()
            val finalTitle = if (note.isEmpty()) category else note // Use category name if note is empty

            saveExpense(finalTitle, amount, category, userEmail)
        }
    }

    private fun saveExpense(title: String, amount: Double, category: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = ExpenseCreateRequest(title, amount, category, email)
                val response = RetrofitClient.instance.addExpense(request)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, "Expense Saved!", Toast.LENGTH_SHORT).show()
                    finish() // Close screen and go back to list
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}