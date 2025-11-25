package com.example.finwise

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.finwise.api.ExpenseCreateRequest
import com.example.finwise.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etNote: EditText
    private lateinit var btnSave: AppCompatButton
    private lateinit var btnBack: ImageView
    private var userEmail: String? = null

    // Categories matching our icon set
    private val categories = listOf(
        "Food", "Shopping", "Transport", "Entertainment", "Utilities", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Initialize Views
        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Get Email passed from BudgetActivity
        userEmail = intent.getStringExtra("USER_EMAIL")

        setupCategorySpinner()

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveExpense() }
    }

    private fun setupCategorySpinner() {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            override fun isEnabled(position: Int): Boolean {
                return true
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val tv = view as TextView
                tv.setTextColor(Color.BLACK)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString().trim()
        val category = categories[spinnerCategory.selectedItemPosition]
        // Note is optional, use title case category as default title if empty
        val title = etNote.text.toString().trim().ifEmpty { category }

        // Basic Validation
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (userEmail == null) {
            Toast.makeText(this, "User email not found, please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- THE FIX IS HERE ---
        // We must convert the string to a Float, not a Double.
        val amountValue: Float
        try {
            amountValue = amountStr.toFloat()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }

        // Create API Request Object
        val request = ExpenseCreateRequest(
            title = title,
            amount = amountValue,
            category = category,
            email = userEmail!!
        )

        // Disable button to prevent double-clicks
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        // Call API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.addExpense(request)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, response.message, Toast.LENGTH_SHORT).show()
                    // Close activity and go back to budget screen
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Re-enable button on failure
                    btnSave.isEnabled = true
                    btnSave.text = "Save Expense"
                }
            }
        }
    }
}