package com.example.finwise

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.CategorySummaryResponse
import com.example.finwise.api.ExpenseResponse
import com.example.finwise.api.RetrofitClient
import com.example.finwise.data.ServiceLocator
import com.example.finwise.data.local.entity.LocalExpense
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetFragment : Fragment(), OnChartValueSelectedListener {

    // UI Components
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var tvRemaining: TextView
    private lateinit var chipGroupRange: ChipGroup
    private lateinit var pieChart: PieChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpensesAdapter
    private lateinit var btnViewBreakdown: MaterialButton

    // Data
    private var userEmail: String? = null
    private var currentRange = "1m" // Default to 1 Month
    private var totalSpentString: String = ""
    private var currentCategoryList: List<CategorySummaryResponse> = emptyList()
    private var totalSpentAmount: Float = 0f

    // Pastel Colors
    private val chartColors: List<Int> = listOf(
        Color.parseColor("#FFB74D"), // Orange (Food)
        Color.parseColor("#CE93D8"), // Purple (Shopping)
        Color.parseColor("#4FC3F7"), // Light Blue (Transport)
        Color.parseColor("#80CBC4"), // Teal (Entertainment)
        Color.parseColor("#FFF176"), // Yellow (Other)
        Color.parseColor("#E57373")  // Red (Utilities)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupRecyclerView()
        setupChartStyle()
        setupChipListeners()

        // 4. Setup Breakdown Button (Placeholder for now)
        btnViewBreakdown.setOnClickListener {
            Toast.makeText(requireContext(), "Full breakdown coming soon!", Toast.LENGTH_SHORT).show()
        }

        // OFFLINE-FIRST: Observe local database (Room) for expenses
        observeLocalExpenses()

        // Trigger initial data load (Both local + API refresh)
        loadDataForRange(currentRange)
    }

    private fun initializeViews(view: View) {
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent)
        progressBarBudget = view.findViewById(R.id.progressBarBudget)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        chipGroupRange = view.findViewById(R.id.chipGroupRange)
        pieChart = view.findViewById(R.id.pieChart)
        recyclerView = view.findViewById(R.id.rvExpenses)
        btnViewBreakdown = view.findViewById(R.id.btnViewBreakdown)
    }

    private fun setupRecyclerView() {
        adapter = ExpensesAdapter(emptyList()) { clickedExpense ->
            // When an item is clicked, open the options sheet
            showExpenseOptions(clickedExpense)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupChartStyle() {
        pieChart.apply {
            description.isEnabled = false
            setOnChartValueSelectedListener(this@BudgetFragment)
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 70f
            transparentCircleRadius = 75f
            setDrawEntryLabels(false)
            setNoDataText("Loading chart...")
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e is PieEntry) {
            pieChart.centerText = "${e.label}\n(Selected)"
        }
    }

    override fun onNothingSelected() {
        pieChart.centerText = "Total Spent\n$totalSpentString"
    }

    private fun setupChipListeners() {
        chipGroupRange.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds[0]
                currentRange = when (checkedId) {
                    R.id.chipToday -> "today"
                    R.id.chip7d -> "7d"
                    R.id.chip1m -> "1m"
                    R.id.chip6m -> "6m"
                    R.id.chip1y -> "1y"
                    R.id.chipAll -> "all"
                    else -> "1m"
                }
                loadDataForRange(currentRange)
            }
        }
    }

    // =====================================================================
    // OFFLINE-FIRST: Observe Room Database for expenses
    // =====================================================================
    private fun observeLocalExpenses() {
        val repository = ServiceLocator.getExpenseRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.allExpenses.collectLatest { localExpenses ->
                    if (isAdded) {
                        // Convert LocalExpense to ExpenseResponse for adapter compatibility
                        val expenseResponses = localExpenses.map { it.toExpenseResponse() }
                        adapter.updateData(expenseResponses)

                        // Update chart data from local expenses
                        updateChartFromLocalData(localExpenses)
                    }
                }
            }
        }
    }

    /**
     * Convert LocalExpense to ExpenseResponse for UI compatibility.
     */
    private fun LocalExpense.toExpenseResponse(): ExpenseResponse {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date(this.dateTimestamp))
        return ExpenseResponse(
            id = this.backendId?.toIntOrNull() ?: this.localId.toInt(),
            title = this.description,
            amount = this.amount.toFloat(),
            category = this.category,
            date = dateStr
        )
    }

    /**
     * Update pie chart from local expenses data.
     */
    private fun updateChartFromLocalData(expenses: List<LocalExpense>) {
        val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

        // Group by category and calculate totals
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalSpent = categoryTotals.values.sum()
        totalSpentAmount = totalSpent.toFloat()
        totalSpentString = rupeeFormat.format(totalSpent)
        tvTotalSpent.text = totalSpentString

        // Assume budget limit from SharedPreferences or default
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val budgetLimit = sharedPref.getFloat("BUDGET_LIMIT", 20000f)

        val remaining = budgetLimit - totalSpent
        tvRemaining.text = "${rupeeFormat.format(remaining)} left to spend"

        val progress = if (budgetLimit > 0) {
            ((totalSpent / budgetLimit) * 100).toInt().coerceIn(0, 100)
        } else 0
        progressBarBudget.progress = progress

        // Build pie chart entries
        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryTotals) {
            if (amount > 0) {
                val label = "$category\n${rupeeFormat.format(amount)}"
                entries.add(PieEntry(amount.toFloat(), label))
            }
        }

        if (entries.isEmpty()) {
            pieChart.data = null
            pieChart.centerText = "No Spending\nFor this period"
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = chartColors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 10f
        dataSet.valueTextSize = 0f

        pieChart.data = PieData(dataSet)
        pieChart.centerText = "Total Spent\n$totalSpentString"
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.parseColor("#333333"))
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.animateY(500)
    }

    // =====================================================================
    // Data Loading: Trigger API refresh in background
    // =====================================================================

    private fun loadDataForRange(range: String) {
        userEmail?.let { email ->
            // Trigger background refresh from API -> updates Room DB -> Flow emits -> UI updates
            refreshFromApi(email)
        }
    }

    /**
     * Refresh data from API in the background.
     * The UI updates automatically via Room Flow observation.
     */
    private fun refreshFromApi(email: String) {
        val repository = ServiceLocator.getExpenseRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshExpenses(email)
        }
    }

    // --- SHOW OPTIONS SHEET ---
    // --- SHOW OPTIONS SHEET (UPDATED FOR EDIT) ---
    private fun showExpenseOptions(expense: ExpenseResponse) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_expense_actions, null)
        dialog.setContentView(sheetView)

        val btnEdit = sheetView.findViewById<LinearLayout>(R.id.optionEdit)
        val btnDelete = sheetView.findViewById<LinearLayout>(R.id.optionDelete)

        btnEdit.setOnClickListener {
            dialog.dismiss()
            // Launch AddExpenseActivity in "Edit Mode" by passing existing data
            val intent = Intent(requireContext(), AddExpenseActivity::class.java).apply {
                // Tell it we are in edit mode and pass the ID
                putExtra("MODE", "EDIT")
                putExtra("EXPENSE_ID", expense.id)
                // Pass the data to pre-fill
                putExtra("EXPENSE_TITLE", expense.title)
                // Pass amount as Float
                putExtra("EXPENSE_AMOUNT", expense.amount)
                putExtra("EXPENSE_CATEGORY", expense.category)
                // We also need the email to save it back later
                putExtra("USER_EMAIL", userEmail)
            }
            startActivity(intent)
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            // Show confirmation dialog before deleting
            showDeleteConfirmation(expense)
        }

        dialog.show()
    }
    // ---------------------------

    // --- SHOW DELETE CONFIRMATION ---
    private fun showDeleteConfirmation(expense: ExpenseResponse) {
        // Use the custom theme created in themes.xml
        AlertDialog.Builder(requireContext(), R.style.FinWiseAlertDialogTheme)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete '${expense.title}'? This cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteExpense(expense.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    // ---------------------------------

    // --- DELETE EXPENSE API CALL (DEBUG VERSION) ---
    private fun deleteExpense(expenseId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.deleteExpense(expenseId)
                withContext(Dispatchers.Main) {
                    if(isAdded) {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                        // REFRESH THE DATA
                        loadDataForRange(currentRange)
                    }
                }
            } catch (e: Exception) {
                // --- DEBUGGING SECTION ---
                e.printStackTrace() // Print full error to Logcat
                val errorMessage = e.message ?: "Unknown error"
                withContext(Dispatchers.Main) {
                    if(isAdded) {
                        // Show the ACTUAL error message on screen
                        Toast.makeText(requireContext(), "Delete Failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
                // -------------------------
            }
        }
    }
    // -------------------------------
}