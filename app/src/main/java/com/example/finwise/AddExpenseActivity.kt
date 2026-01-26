package com.example.finwise

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
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
import java.util.TimeZone

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etNote: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSave: AppCompatButton
    private lateinit var btnBack: android.widget.ImageView
    private lateinit var tvHeaderTitle: TextView // Header title TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private var userEmail: String? = null

    private val selectedCalendar = Calendar.getInstance()

    // Your existing category list
    private val categories = listOf(
        "Food", "Shopping", "Transport", "Entertainment", "Utilities", "Health", "Housing", "Other"
    )

    // Variables for edit mode
    private var isEditMode = false
    private var expenseIdToEdit: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        initializeViews()
        setupCategorySpinner()
        setupDatePicker()

        userEmail = intent.getStringExtra("USER_EMAIL")

        // --- NEW: Check for EDIT Mode ---
        val mode = intent.getStringExtra("MODE")
        if (mode == "EDIT") {
            isEditMode = true
            expenseIdToEdit = intent.getIntExtra("EXPENSE_ID", -1)
            val title = intent.getStringExtra("EXPENSE_TITLE")
            val amount = intent.getFloatExtra("EXPENSE_AMOUNT", 0f)
            val category = intent.getStringExtra("EXPENSE_CATEGORY")
            val dateString = intent.getStringExtra("EXPENSE_DATE")

            setupEditMode(title, amount, category, dateString)
        }
        // --- NEW: Check for SMS Auto-fill ---
        else if (intent.hasExtra("EXTRA_AMOUNT")) {
            val amountStr = intent.getStringExtra("EXTRA_AMOUNT")
            val title = intent.getStringExtra("EXTRA_TITLE")
            val category = intent.getStringExtra("EXTRA_CATEGORY")
            
            // Pre-fill amount
            amountStr?.let { etAmount.setText(it) }
            
            // Pre-fill title/note
            title?.let { etNote.setText(it) }
            
            // Pre-select category in spinner
            category?.let { cat ->
                val index = categories.indexOfFirst { it.equals(cat, ignoreCase = true) }
                if (index != -1) {
                    spinnerCategory.setSelection(index)
                }
            }
        }
        // --------------------------------

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { handleSaveButtonClick() }
    }

    private fun initializeViews() {
        etAmount = findViewById(R.id.etAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etNote = findViewById(R.id.etNote)
        etDate = findViewById(R.id.etDate)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        // Assuming you have a TextView with this ID in your layout for the header
        // If not, you might need to add it or find the correct ID.
        // Based on typical layouts, it's often named 'tvHeaderTitle' or similar.
        // Let's try finding it, if it crashes, we can adjust the ID.
        try {
            tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        } catch (e: Exception) {
            // Handle case where ID might be different or missing
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, categories)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        // Only set default date if NOT in edit mode (edit mode sets its own date)
        if (!isEditMode) {
            updateDateLabel()
        }

        etDate.setOnClickListener {
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

    // --- Helper to populate UI for Edit Mode ---
    private fun setupEditMode(title: String?, amount: Float, category: String?, dateString: String?) {
        // 1. Change header and button text
        try {
            tvHeaderTitle.text = "Edit Expense"
        } catch (e: Exception) { /* Ignore if view not found */ }
        btnSave.text = "Update Expense"

        // 2. Pre-fill inputs
        etNote.setText(title)
        etAmount.setText(amount.toString())

        // 3. Select the correct category in the spinner
        category?.let {
            // Find case-insensitive match
            val index = categories.indexOfFirst { cat -> cat.equals(it, ignoreCase = true) }
            if (index != -1) {
                spinnerCategory.setSelection(index)
            }
        }

        // 4. Set the date
        dateString?.let { parseAndSetDate(it) }
    }

    private fun parseAndSetDate(dateString: String) {
        try {
            // Try parsing the full ISO format first
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            isoFormat.timeZone = TimeZone.getTimeZone("UTC") // API sends UTC
            val date = isoFormat.parse(dateString)
            if (date != null) {
                selectedCalendar.time = date
            }
        } catch (e: Exception) {
            try {
                // Fallback to simpler format if milliseconds/time are missing
                val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = simpleFormat.parse(dateString.substringBefore("T"))
                if (date != null) {
                    selectedCalendar.time = date
                }
            } catch (e2: Exception) {
                // If parsing fails, just leave it as today's date
            }
        }
        updateDateLabel()
    }
    // -------------------------------------------

    // --- Main Save Handler ---
    private fun handleSaveButtonClick() {
        val amountStr = etAmount.text.toString().trim()
        val category = categories[spinnerCategory.selectedItemPosition]
        // Use category as title if note is empty
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
            if (amountValue <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show()
            return
        }

        // Format date for backend (ISO 8601)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val dateStringForBackend = isoFormat.format(selectedCalendar.time)

        val request = ExpenseCreateRequest(
            title = title,
            amount = amountValue,
            category = category,
            email = userEmail!!,
            date = dateStringForBackend
        )

        showLoadingState()

        if (isEditMode) {
            updateExpense(expenseIdToEdit, request)
        } else {
            addExpense(request)
        }
    }
    // -------------------------

    // --- API CALL: UPDATE EXISTING EXPENSE ---
    private fun updateExpense(id: Int, request: ExpenseCreateRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.updateExpense(id, request)
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    Toast.makeText(this@AddExpenseActivity, "Expense updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity only after successful response
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    Toast.makeText(this@AddExpenseActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    // Don't finish - let user retry
                }
            }
        }
    }
    // -----------------------------------------

    // --- API CALL: ADD NEW EXPENSE ---
    private fun addExpense(request: ExpenseCreateRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.addExpense(request)
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    Toast.makeText(this@AddExpenseActivity, "Saved successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity only after successful response
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingState()
                    Toast.makeText(this@AddExpenseActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                    // Don't finish - let user retry
                }
            }
        }
    }

    private fun showLoadingState() {
        btnSave.isEnabled = false
        btnSave.text = if (isEditMode) "Updating..." else "Saving..."
        progressBar.visibility = android.view.View.VISIBLE
    }

    private fun hideLoadingState() {
        progressBar.visibility = android.view.View.GONE
        btnSave.isEnabled = true
        btnSave.text = if (isEditMode) "Update Expense" else "Save Expense"
    }
}