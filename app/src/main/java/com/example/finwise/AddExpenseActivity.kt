package com.example.finwise

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.finwise.api.ExpenseCreateRequest
import com.example.finwise.api.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etNote: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSave: AppCompatButton
    private lateinit var btnBack: android.widget.ImageView
    private var userEmail: String? = null

    private val selectedCalendar = Calendar.getInstance()

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
        etDate = findViewById(R.id.etDate)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        userEmail = intent.getStringExtra("USER_EMAIL")

        setupCategorySpinner()
        setupDatePicker()

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveExpense() }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        updateDateLabel()

        etDate.setOnClickListener {
            // Pass the custom theme here as the second argument
            DatePickerDialog(
                this,
                R.style.Theme_FinWise_DatePickerDialog,
                { _, year, month, dayOfMonth ->
                    selectedCalendar.set(Calendar.YEAR, year)
                    selectedCalendar.set(Calendar.MONTH, month)
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateLabel()
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel() {
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        etDate.setText(displayFormat.format(selectedCalendar.time))
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString().trim()
        val category = categories[spinnerCategory.selectedItemPosition]
        val title = etNote.text.toString().trim().ifEmpty { category }

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (userEmail == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val amountValue: Float
        try {
            amountValue = amountStr.toFloat()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val dateStringForBackend = isoFormat.format(selectedCalendar.time)

        val request = ExpenseCreateRequest(
            title = title,
            amount = amountValue,
            category = category,
            email = userEmail!!,
            date = dateStringForBackend
        )

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.addExpense(request)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, response.message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddExpenseActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Save Expense"
                }
            }
        }
    }
}