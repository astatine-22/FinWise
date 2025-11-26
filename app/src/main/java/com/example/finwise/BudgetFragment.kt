package com.example.finwise

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

class BudgetFragment : Fragment(), OnChartValueSelectedListener {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var rvExpenses: RecyclerView
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var pieChart: PieChart
    private lateinit var btnViewBreakdown: MaterialButton
    private lateinit var chipGroupRange: ChipGroup

    private var totalSpentString: String = ""
    private var currentCategoryList: List<CategorySummaryResponse> = emptyList()
    private var totalSpentAmount: Float = 0f
    private var currentRange: String = "1m"
    private var userEmail: String? = null

    private val chartColors: List<Int> = listOf(
        Color.parseColor("#FFB74D"), Color.parseColor("#CE93D8"),
        Color.parseColor("#4FC3F7"), Color.parseColor("#80CBC4"),
        Color.parseColor("#FFF176"), Color.parseColor("#E57373")
    )

    // 1. Inflate layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Tell the fragment which XML layout to use
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    // 2. Initialize logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user email from parent activity
        userEmail = requireActivity().intent.getStringExtra("USER_EMAIL")

        // Initialize Views using 'view.'
        tvTotalSpent = view.findViewById(R.id.tvTotalSpent)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        progressBarBudget = view.findViewById(R.id.progressBarBudget)
        rvExpenses = view.findViewById(R.id.rvExpenses)
        pieChart = view.findViewById(R.id.pieChart)
        btnViewBreakdown = view.findViewById(R.id.btnViewBreakdown)
        chipGroupRange = view.findViewById(R.id.chipGroupRange)

        setupRecyclerView()
        setupPieChartStyle()
        setupRangeChips()

        btnViewBreakdown.setOnClickListener {
            showBottomSheetBreakdown()
        }
    }

    override fun onResume() {
        super.onResume()
        loadBudgetData()
    }

    private fun loadBudgetData() {
        userEmail?.let {
            fetchBudgetSummary(it, currentRange)
            fetchExpenses(it, currentRange)
            fetchCategorySummary(it, currentRange)
        }
    }

    private fun setupRangeChips() {
        chipGroupRange.setOnCheckedChangeListener { _, checkedId ->
            currentRange = when (checkedId) {
                R.id.chipToday -> "today"
                R.id.chip7d -> "7d"
                R.id.chip1m -> "1m"
                R.id.chip6m -> "6m"
                R.id.chip1y -> "1y"
                R.id.chipAll -> "all"
                else -> "1m"
            }
            loadBudgetData()
        }
    }


    private fun setupRecyclerView() {
        // Use requireContext() instead of this
        expenseAdapter = ExpenseAdapter(emptyList())
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvExpenses.adapter = expenseAdapter
    }

    private fun setupPieChartStyle() {
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
        if (e is PieEntry) pieChart.centerText = "${e.label}\n(Selected)"
    }

    override fun onNothingSelected() {
        pieChart.centerText = "Total Spent\n$totalSpentString"
    }


    private fun showBottomSheetBreakdown() {
        // Use requireContext() for Dialogs
        if (currentCategoryList.isEmpty() || totalSpentAmount == 0f) {
            Toast.makeText(requireContext(), "No data to display", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_breakdown, null)
        dialog.setContentView(view)

        val rvBreakdown = view.findViewById<RecyclerView>(R.id.rvBreakdown)
        rvBreakdown.layoutManager = LinearLayoutManager(requireContext())

        val breakdownItems = ArrayList<BreakdownItem>()
        var colorIndex = 0
        for (item in currentCategoryList) {
            if (item.total_amount > 0) {
                val percentage = ((item.total_amount / totalSpentAmount) * 100).toInt()
                val color = chartColors[colorIndex % chartColors.size]
                breakdownItems.add(BreakdownItem(item.category, item.total_amount, color, percentage))
                colorIndex++
            }
        }
        rvBreakdown.adapter = CategoryBreakdownAdapter(breakdownItems)
        dialog.show()
    }

    // --- API CALLS (Checks isAdded to prevent crashes) ---

    private fun fetchBudgetSummary(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = RetrofitClient.instance.getBudgetSummary(email, range)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
                        totalSpentAmount = summary.total_spent
                        totalSpentString = rupeeFormat.format(summary.total_spent)
                        tvTotalSpent.text = totalSpentString
                        val remaining = summary.limit - summary.total_spent
                        tvRemaining.text = "${rupeeFormat.format(remaining)} left to spend"
                        val progress = if (summary.limit > 0) ((summary.total_spent / summary.limit) * 100).toInt().coerceIn(0, 100) else 0
                        progressBarBudget.progress = progress
                        pieChart.centerText = "Total Spent\n$totalSpentString"
                        pieChart.highlightValues(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) Toast.makeText(requireContext(), "Error loading summary", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchExpenses(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val expensesList = RetrofitClient.instance.getExpenses(email, range)
                withContext(Dispatchers.Main) {
                    if (isAdded) expenseAdapter.updateData(expensesList)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { if (isAdded) expenseAdapter.updateData(emptyList()) }
            }
        }
    }

    private fun fetchCategorySummary(email: String, range: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoryList = RetrofitClient.instance.getCategorySummary(email, range)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        currentCategoryList = categoryList
                        updatePieChartData(categoryList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        pieChart.setNoDataText("No data.")
                        pieChart.invalidate()
                    }
                }
            }
        }
    }

    private fun updatePieChartData(categoryList: List<CategorySummaryResponse>) {
        val entries = ArrayList<PieEntry>()
        for (item in categoryList) {
            if (item.total_amount > 0) {
                val rupeeFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
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
        pieChart.animateY(500)
    }
}