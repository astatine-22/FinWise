package com.example.finwise

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.CategorySummaryResponse
import com.example.finwise.api.RetrofitClient
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BudgetActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var rvExpenses: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var pieChart: PieChart
    private lateinit var btnViewBreakdown: MaterialButton
    private lateinit var chipGroupRange: ChipGroup

    private var totalSpentString: String = ""
    // Holds data for the bottom sheet
    private var currentCategoryList: List<CategorySummaryResponse> = emptyList()
    private var totalSpentAmount: Float = 0f
    // Default date range is 1 month
    private var currentRange: String = "1m"

    // Define the specific pastel colors from your design
    private val chartColors: List<Int> = listOf(
        Color.parseColor("#FFB74D"), // Orange (Food)
        Color.parseColor("#CE93D8"), // Purple (Shopping)
        Color.parseColor("#4FC3F7"), // Light Blue (Transport)
        Color.parseColor("#80CBC4"), // Teal (Entertainment)
        Color.parseColor("#FFF176"), // Yellow (Other)
        Color.parseColor("#E57373")  // Red (Utilities)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Initialize Views
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        tvRemaining = findViewById(R.id.tvRemaining)
        progressBarBudget = findViewById(R.id.progressBarBudget)
        rvExpenses = findViewById(R.id.rvExpenses)
        pieChart = findViewById(R.id.pieChart)
        btnViewBreakdown = findViewById(R.id.btnViewBreakdown)
        chipGroupRange = findViewById(R.id.chipGroupRange)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val fabAdd = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)

        setupRecyclerView()
        setupPieChartStyle()
        setupRangeChips()

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: return

        // Buttons
        btnBack.setOnClickListener { finish() }
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

        btnViewBreakdown.setOnClickListener {
            showBottomSheetBreakdown()
        }
    }

    override fun onResume() {
        super.onResume()
        // Load data using whatever range is currently selected
        loadBudgetData()
    }

    // --- Helper to load all data based on current range ---
    private fun loadBudgetData() {
        val userEmail = intent.getStringExtra("USER_EMAIL")
        if (userEmail != null) {
            // Pass the currentRange variable to the API calls
            fetchBudgetSummary(userEmail, currentRange)
            fetchExpenses(userEmail, currentRange)
            fetchCategorySummary(userEmail, currentRange)
        }
    }

    // --- Setup Chip Listeners ---
    private fun setupRangeChips() {
        chipGroupRange.setOnCheckedChangeListener { _, checkedId ->
            // Determine range string based on which chip ID is checked
            currentRange = when (checkedId) {
                R.id.chipToday -> "today"
                R.id.chip7d -> "7d"
                R.id.chip1m -> "1m"
                R.id.chip6m -> "6m"
                R.id.chip1y -> "1y"
                R.id.chipAll -> "all"
                else -> "1m" // Default fallback
            }
            // Reload all data with the new range
            loadBudgetData()
        }
    }


    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(emptyList())
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter
    }

    // --- Configure Pie Chart Appearance ---
    private fun setupPieChartStyle() {
        pieChart.apply {
            description.isEnabled = false
            setOnChartValueSelectedListener(this@BudgetActivity)
            legend.isEnabled = false // Hide default legend
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 70f
            transparentCircleRadius = 75f
            setDrawEntryLabels(false)
            contentDescription = "Spending by category pie chart"
            setNoDataText("Loading chart...")
        }
    }

    // --- Handle Chart Clicks ---
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e is PieEntry) {
            // Show selected category and amount in center
            pieChart.centerText = "${e.label}\n(Selected)"
        }
    }

    override fun onNothingSelected() {
        // Reset to total when deselected
        pieChart.centerText = "Total Spent\n$totalSpentString"
    }
    // -------------------------------------------


    // --- Function to show Bottom Sheet ---
    private fun showBottomSheetBreakdown() {
        if (currentCategoryList.isEmpty() || totalSpentAmount == 0f) {
            Toast.makeText(this, "No data to display for this period", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_breakdown, null)
        dialog.setContentView(view)

        val rvBreakdown = view.findViewById<RecyclerView>(R.id.rvBreakdown)
        rvBreakdown.layoutManager = LinearLayoutManager(this)

        val breakdownItems = ArrayList<BreakdownItem>()
        var colorIndex = 0
        for (item in currentCategoryList) {
            if (item.total_amount > 0) {
                // Calculate percentage for the sheet
                val percentage = ((item.total_amount / totalSpentAmount) * 100).toInt()
                // Assign colors cyclically
                val color = chartColors[colorIndex % chartColors.size]
                breakdownItems.add(BreakdownItem(item.category, item.total_amount, color, percentage))
                colorIndex++
            }
        }

        val adapter = CategoryBreakdownAdapter(breakdownItems)
        rvBreakdown.adapter = adapter

        dialog.show()
    }
    // ------------------------------------------


    // --- API CALLS (Accepting range) ---

    private fun fetchBudgetSummary(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = RetrofitClient.instance.getBudgetSummary(email, range)
                withContext(Dispatchers.Main) {
                    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

                    totalSpentAmount = summary.total_spent
                    totalSpentString = rupeeFormat.format(summary.total_spent)

                    tvTotalSpent.text = totalSpentString

                    val remaining = summary.limit - summary.total_spent
                    tvRemaining.text = "${rupeeFormat.format(remaining)} left to spend"

                    val progress = if (summary.limit > 0) {
                        ((summary.total_spent / summary.limit) * 100).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    progressBarBudget.progress = progress

                    pieChart.centerText = "Total Spent\n$totalSpentString"
                    pieChart.setCenterTextSize(16f)
                    pieChart.setCenterTextColor(Color.parseColor("#333333"))
                    // Ensure chart text resets when range changes
                    pieChart.highlightValues(null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BudgetActivity, "Error loading budget summary", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchExpenses(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val expensesList = RetrofitClient.instance.getExpenses(email, range)
                withContext(Dispatchers.Main) {
                    // Toast debug message removed from here
                    expenseAdapter.updateData(expensesList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    expenseAdapter.updateData(emptyList())
                }
            }
        }
    }

    private fun fetchCategorySummary(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryList = RetrofitClient.instance.getCategorySummary(email, range)
                withContext(Dispatchers.Main) {
                    currentCategoryList = categoryList
                    updatePieChartData(categoryList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pieChart.setNoDataText("No chart data for this period.")
                    currentCategoryList = emptyList()
                    pieChart.data = null
                    pieChart.invalidate()
                }
            }
        }
    }

    private fun updatePieChartData(categoryList: List<CategorySummaryResponse>) {
        val entries = ArrayList<PieEntry>()

        for (item in categoryList) {
            if (item.total_amount > 0) {
                val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
                // Label combined with amount for chart slice click interaction
                val label = "${item.category}\n${rupeeFormat.format(item.total_amount)}"
                entries.add(PieEntry(item.total_amount.toFloat(), label))
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

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.centerText = "Total Spent\n$totalSpentString"
        pieChart.highlightValues(null)
        pieChart.invalidate()
        pieChart.animateY(500) // Faster animation when switching ranges
    }
}