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
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var tvRemaining: TextView
    private lateinit var chipGroupRange: ChipGroup
    private lateinit var pieChart: PieChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpensesAdapter
    private lateinit var btnViewBreakdown: MaterialButton
    private lateinit var layoutEmptyState: View

    // Data - MUTABLE LIST FOR STRICT CLEARING
    private var userEmail: String? = null
    private var currentRange = "today" // Default to Today (current date)
    private var totalSpentString: String = ""
    private var currentCategoryList: List<CategorySummaryResponse> = emptyList()
    private var totalSpentAmount: Float = 0f
    
    // Mutable list to prevent duplicates - cleared on every update
    private val expenseList = mutableListOf<ExpenseResponse>()
    
    // Cache all expenses for re-filtering when range changes
    private var allExpensesData: List<LocalExpense> = emptyList()

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
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent)
        progressBarBudget = view.findViewById(R.id.progressBarBudget)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        chipGroupRange = view.findViewById(R.id.chipGroupRange)
        pieChart = view.findViewById(R.id.pieChart)
        recyclerView = view.findViewById(R.id.rvExpenses)
        btnViewBreakdown = view.findViewById(R.id.btnViewBreakdown)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        
        // Setup pull-to-refresh
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        // Use the mutable list for the adapter
        adapter = ExpensesAdapter(expenseList) { clickedExpense ->
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
                // Immediately refresh the view with new filter
                refreshCurrentView()
                // Also trigger API refresh in background
                loadDataForRange(currentRange)
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.finwise_green, R.color.goals_purple)
        swipeRefreshLayout.setOnRefreshListener {
            userEmail?.let { email ->
                refreshFromApi(email)
            } ?: run {
                swipeRefreshLayout.isRefreshing = false
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
                        // Cache all expenses for re-filtering
                        allExpensesData = localExpenses
                        
                        // STRICT MODE: Clear list before adding new data to prevent duplicates
                        expenseList.clear()
                        
                        // Filter expenses based on current range
                        val filteredExpenses = filterExpensesByRange(localExpenses, currentRange)
                        
                        // Convert LocalExpense to ExpenseResponse and add to mutable list
                        val expenseResponses = filteredExpenses.map { it.toExpenseResponse() }
                        
                        // Toggle empty state visibility
                        if (expenseResponses.isEmpty()) {
                            // Show empty state, hide RecyclerView
                            layoutEmptyState.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                        } else {
                            // Hide empty state, show RecyclerView
                            layoutEmptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            
                            // Add all filtered expenses to the cleared list
                            expenseList.addAll(expenseResponses)
                        }
                        
                        // Notify adapter of changes
                        adapter.notifyDataSetChanged()

                        // Update chart data from filtered local expenses
                        updateChartFromLocalData(filteredExpenses)
                    }
                }
            }
        }
    }
    
    /**
     * Manually refresh the current view with updated filter
     * Called when user changes date range via chips
     */
    private fun refreshCurrentView() {
        // Use cached data to immediately re-filter and update UI
        if (isAdded && allExpensesData.isNotEmpty()) {
            // STRICT MODE: Clear list before adding
            expenseList.clear()
            
            // Filter based on new range
            val filteredExpenses = filterExpensesByRange(allExpensesData, currentRange)
            
            // Convert and add to list
            val expenseResponses = filteredExpenses.map { it.toExpenseResponse() }
            
            // Toggle empty state visibility
            if (expenseResponses.isEmpty()) {
                layoutEmptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                expenseList.addAll(expenseResponses)
            }
            
            // Notify adapter
            adapter.notifyDataSetChanged()
            
            // Update chart
            updateChartFromLocalData(filteredExpenses)
        }
    }

    /**
     * Filter expenses based on the selected date range.
     */
    private fun filterExpensesByRange(expenses: List<LocalExpense>, range: String): List<LocalExpense> {
        val calendar = java.util.Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        // Calculate the start time based on range
        val startTime = when (range) {
            "today" -> {
                // Start of today (00:00:00)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "7d" -> {
                // Last 7 days
                currentTime - (7L * 24 * 60 * 60 * 1000)
            }
            "1m" -> {
                // Start of current month
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "6m" -> {
                // Last 6 months
                currentTime - (6L * 30 * 24 * 60 * 60 * 1000)
            }
            "1y" -> {
                // Last 1 year
                currentTime - (365L * 24 * 60 * 60 * 1000)
            }
            "all" -> {
                // Show all expenses (no filtering)
                return expenses
            }
            else -> currentTime - (30L * 24 * 60 * 60 * 1000) // Default to 1 month
        }
        
        // Filter expenses where dateTimestamp is >= startTime and <= currentTime
        return expenses.filter { it.dateTimestamp in startTime..currentTime }
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
        
        // Show indicator if not started by swipe
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.refreshExpenses(email)
            } catch (e: Exception) {
                // Handle error (optional toast)
            } finally {
                // Hide loading indicator
                swipeRefreshLayout.isRefreshing = false
            }
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