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
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var rvExpenses: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    // PieChart view variable
    private lateinit var pieChart: PieChart

    // Define the specific pastel colors from your design
    // We explicitly type this as List<Int> to avoid type inference issues
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
        pieChart = findViewById(R.id.pieChart) // Initialize PieChart
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
        // Explicitly initialize with an empty list of ExpenseResponse
        expenseAdapter = ExpenseAdapter(emptyList())
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter
    }

    // --- Configure Pie Chart Appearance ---
    private fun setupPieChartStyle() {
        pieChart.apply {
            description.isEnabled = false // Disable description text
            legend.isEnabled = true // Show the legend below
            legend.textSize = 14f
            legend.textColor = Color.parseColor("#757575")
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.yEntrySpace = 10f
            legend.xEntrySpace = 20f
            // Removed wordWrapEnabled

            // Make it a donut chart
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 70f // Size of the hole
            transparentCircleRadius = 75f

            setDrawEntryLabels(false) // Hide labels on the chart slices themselves
            contentDescription = "Spending by category pie chart"
        }
    }
    // -------------------------------------------


    // --- API CALLS ---

    private fun fetchBudgetSummary(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = RetrofitClient.instance.getBudgetSummary(email)
                withContext(Dispatchers.Main) {
                    // --- FIX: Use forLanguageTag ---
                    val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

                    tvTotalSpent.text = rupeeFormat.format(summary.total_spent)

                    val remaining = summary.limit - summary.total_spent
                    tvRemaining.text = "${rupeeFormat.format(remaining)} left to spend"

                    // Ensure progress is a valid integer between 0 and 100
                    val progress = if (summary.limit > 0) {
                        ((summary.total_spent / summary.limit) * 100).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    progressBarBudget.progress = progress

                    // Update center text of Pie Chart
                    pieChart.centerText = "Total Spent\n${rupeeFormat.format(summary.total_spent)}"
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
                // Handle error quietly or show toast
            }
        }
    }

    // --- Fetch and display Chart Data ---
    private fun fetchCategorySummary(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryList = RetrofitClient.instance.getCategorySummary(email)
                withContext(Dispatchers.Main) {
                    updatePieChartData(categoryList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Optional: handle chart loading error
                    pieChart.setNoDataText("Could not load chart data.")
                    pieChart.invalidate()
                }
            }
        }
    }

    private fun updatePieChartData(categoryList: List<CategorySummaryResponse>) {
        // Explicitly declare the type of ArrayList to avoid inference errors
        val entries = ArrayList<PieEntry>()

        // Map API response to PieEntries
        for (item in categoryList) {
            if (item.total_amount > 0) {
                // --- FIX: Use forLanguageTag ---
                val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
                val label = "${item.category} (${rupeeFormat.format(item.total_amount)})"

                // We explicitly cast to Float using .toFloat() to satisfy the compiler
                entries.add(PieEntry(item.total_amount.toFloat(), label))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = chartColors // Set our pastel colors
        dataSet.sliceSpace = 3f // White space between slices
        dataSet.selectionShift = 5f
        dataSet.valueTextSize = 0f // Hide numbers on the slices (they are in the legend)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate() // Refresh the chart
        pieChart.animateY(1000) // Nice animation
    }
    // -----------------------------------------
}