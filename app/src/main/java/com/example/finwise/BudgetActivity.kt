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
    // NEW: Button to open Bottom Sheet
    private lateinit var btnViewBreakdown: MaterialButton

    private var totalSpentString: String = ""
    // NEW: Store category data for the bottom sheet
    private var currentCategoryList: List<CategorySummaryResponse> = emptyList()
    private var totalSpentAmount: Float = 0f

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
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val fabAdd = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)

        setupRecyclerView()
        setupPieChartStyle() // Configure chart looks

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: return

        // Buttons
        btnBack.setOnClickListener { finish() }
        fabAdd.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }

        // NEW: Bottom Sheet Button Click Listener
        btnViewBreakdown.setOnClickListener {
            showBottomSheetBreakdown()
        }
    }

    override fun onResume() {
        super.onResume()
        val userEmail = intent.getStringExtra("USER_EMAIL")
        if (userEmail != null) {
            fetchBudgetSummary(userEmail)
            fetchExpenses(userEmail)
            fetchCategorySummary(userEmail) // Fetch chart data
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

            // NEW: Disable the built-in legend completely
            legend.isEnabled = false

            // Donut chart styling
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 70f
            transparentCircleRadius = 75f

            setDrawEntryLabels(false) // Hide labels on slices
            contentDescription = "Spending by category pie chart"
            setNoDataText("Loading chart...")
        }
    }

    // --- Handle Chart Clicks ---
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e is PieEntry) {
            pieChart.centerText = "${e.label}\n(Selected)"
        }
    }

    override fun onNothingSelected() {
        pieChart.centerText = "Total Spent\n$totalSpentString"
    }
    // -------------------------------------------


    // --- NEW: Function to show Bottom Sheet ---
    private fun showBottomSheetBreakdown() {
        if (currentCategoryList.isEmpty() || totalSpentAmount == 0f) {
            Toast.makeText(this, "No data to display", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_breakdown, null)
        dialog.setContentView(view)

        val rvBreakdown = view.findViewById<RecyclerView>(R.id.rvBreakdown)
        rvBreakdown.layoutManager = LinearLayoutManager(this)

        // Prepare data for adapter
        val breakdownItems = ArrayList<BreakdownItem>()
        var colorIndex = 0
        for (item in currentCategoryList) {
            if (item.total_amount > 0) {
                // Calculate percentage
                val percentage = ((item.total_amount / totalSpentAmount) * 100).toInt()
                // Assign color cyclically
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


    // --- API CALLS ---

    private fun fetchBudgetSummary(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = RetrofitClient.instance.getBudgetSummary(email)
                withContext(Dispatchers.Main) {
                    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

                    // Store total amount for percentage calculations
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
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BudgetActivity, "Error loading budget", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchExpenses(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val expensesList = RetrofitClient.instance.getExpenses(email)
                withContext(Dispatchers.Main) {
                    expenseAdapter.updateData(expensesList)
                }
            } catch (e: Exception) {
                // Handle error quietly
            }
        }
    }

    private fun fetchCategorySummary(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryList = RetrofitClient.instance.getCategorySummary(email)
                withContext(Dispatchers.Main) {
                    // Store data for bottom sheet
                    currentCategoryList = categoryList
                    updatePieChartData(categoryList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pieChart.setNoDataText("Could not load chart data.")
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
                // Label for chart slice click
                val label = "${item.category}\n${rupeeFormat.format(item.total_amount)}"
                entries.add(PieEntry(item.total_amount.toFloat(), label))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = chartColors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 10f
        dataSet.valueTextSize = 0f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.centerText = "Total Spent\n$totalSpentString"
        pieChart.highlightValues(null) // Clear previous selection
        pieChart.invalidate()
        pieChart.animateY(1000)
    }
}