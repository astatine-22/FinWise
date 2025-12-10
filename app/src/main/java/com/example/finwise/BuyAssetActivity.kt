package com.example.finwise

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.PriceHistoryResponse
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.StockSearchResult
import com.example.finwise.api.TradeRequest
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BuyAssetActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var etSymbol: EditText
    private lateinit var suggestionsCard: CardView
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var searchSection: LinearLayout
    private lateinit var stockDetailsSection: LinearLayout
    private lateinit var tvCurrentPrice: TextView
    private lateinit var tvPriceChange: TextView
    private lateinit var tvAvailableCash: TextView
    private lateinit var tvHoldings: TextView
    private lateinit var holdingStatsRow: LinearLayout
    private lateinit var tvAvgBuyPrice: TextView
    private lateinit var tvUnrealizedPnl: TextView
    private lateinit var btnMinus: MaterialButton
    private lateinit var btnPlus: MaterialButton
    private lateinit var etQuantity: EditText
    private lateinit var chip25: Chip
    private lateinit var chip50: Chip
    private lateinit var chipMax: Chip
    private lateinit var exchangeToggle: MaterialButtonToggleGroup
    private lateinit var btnNse: MaterialButton
    private lateinit var btnBse: MaterialButton
    private lateinit var periodToggle: MaterialButtonToggleGroup
    private lateinit var btn1D: MaterialButton
    private lateinit var btn1W: MaterialButton
    private lateinit var btn1M: MaterialButton
    private lateinit var priceChart: LineChart
    private lateinit var chartLoading: ProgressBar
    private lateinit var tvEstimatedCost: TextView
    private lateinit var btnBuy: MaterialButton
    private lateinit var btnSell: MaterialButton
    private lateinit var loadingOverlay: FrameLayout

    // Adapters
    private lateinit var suggestionAdapter: StockSuggestionAdapter

    // Data
    private var userEmail: String? = null
    private var currentPrice: Float = 0f
    private var currentSymbol: String = ""
    private var baseSymbol: String = ""
    private var stockName: String = ""
    private var currentExchange: String = "NSE"
    private var isIndianStock: Boolean = false
    private var availableCash: Float = 100000f
    private var currentHoldings: Float = 0f
    private var avgBuyPrice: Float = 0f
    private var currentPeriod: String = "1d"

    // Search debounce
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DELAY_MS = 300L

    // Rupee formatter
    private val rupeeFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_asset)

        userEmail = intent.getStringExtra("USER_EMAIL")
        val prefilledSymbol = intent.getStringExtra("PREFILLED_SYMBOL")

        initializeViews()
        setupChart()
        setupRecyclerView()
        setupListeners()
        loadAvailableCash()

        // Pre-fill symbol if provided
        prefilledSymbol?.let { symbol ->
            baseSymbol = symbol.replace(".NS", "").replace(".BO", "")
            isIndianStock = symbol.endsWith(".NS") || symbol.endsWith(".BO")
            stockName = baseSymbol
            onStockSelected(StockSearchResult(
                name = baseSymbol,
                symbol = baseSymbol,
                exchange = if (isIndianStock) "NSE/BSE" else "US"
            ))
        }
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        etSymbol = findViewById(R.id.etSymbol)
        suggestionsCard = findViewById(R.id.suggestionsCard)
        rvSuggestions = findViewById(R.id.rvSuggestions)
        searchSection = findViewById(R.id.searchSection)
        stockDetailsSection = findViewById(R.id.stockDetailsSection)
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice)
        tvPriceChange = findViewById(R.id.tvPriceChange)
        tvAvailableCash = findViewById(R.id.tvAvailableCash)
        tvHoldings = findViewById(R.id.tvHoldings)
        holdingStatsRow = findViewById(R.id.holdingStatsRow)
        tvAvgBuyPrice = findViewById(R.id.tvAvgBuyPrice)
        tvUnrealizedPnl = findViewById(R.id.tvUnrealizedPnl)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        etQuantity = findViewById(R.id.etQuantity)
        chip25 = findViewById(R.id.chip25)
        chip50 = findViewById(R.id.chip50)
        chipMax = findViewById(R.id.chipMax)
        exchangeToggle = findViewById(R.id.exchangeToggle)
        btnNse = findViewById(R.id.btnNse)
        btnBse = findViewById(R.id.btnBse)
        periodToggle = findViewById(R.id.periodToggle)
        btn1D = findViewById(R.id.btn1D)
        btn1W = findViewById(R.id.btn1W)
        btn1M = findViewById(R.id.btn1M)
        priceChart = findViewById(R.id.priceChart)
        chartLoading = findViewById(R.id.chartLoading)
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost)
        btnBuy = findViewById(R.id.btnBuy)
        btnSell = findViewById(R.id.btnSell)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupChart() {
        priceChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawLabels(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#757575")
                textSize = 10f
            }
            
            axisRight.isEnabled = false
            
            setNoDataText("Select a stock to view chart")
            setNoDataTextColor(Color.parseColor("#757575"))
        }
    }

    private fun setupRecyclerView() {
        suggestionAdapter = StockSuggestionAdapter(emptyList()) { stock ->
            onStockSelected(stock)
        }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = suggestionAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Search with debounce
        etSymbol.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    searchRunnable = Runnable { performSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY_MS)
                } else {
                    hideSuggestions()
                }
            }
        })

        // Quantity +/- buttons
        btnMinus.setOnClickListener {
            val current = etQuantity.text.toString().toFloatOrNull() ?: 1f
            if (current > 1) {
                etQuantity.setText(String.format("%.0f", current - 1))
                calculateEstimatedCost()
            }
        }

        btnPlus.setOnClickListener {
            val current = etQuantity.text.toString().toFloatOrNull() ?: 0f
            etQuantity.setText(String.format("%.0f", current + 1))
            calculateEstimatedCost()
        }

        // Quick percentage chips
        chip25.setOnClickListener { setQuantityPercentage(0.25f) }
        chip50.setOnClickListener { setQuantityPercentage(0.50f) }
        chipMax.setOnClickListener { setQuantityPercentage(1.0f) }

        // Quantity text change
        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateEstimatedCost()
            }
        })

        // Exchange toggle
        exchangeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && baseSymbol.isNotEmpty()) {
                val newExchange = if (checkedId == R.id.btnNse) "NSE" else "BSE"
                if (newExchange != currentExchange) {
                    currentExchange = newExchange
                    val suffix = if (currentExchange == "NSE") ".NS" else ".BO"
                    loadStockData("$baseSymbol$suffix")
                }
            }
        }

        // Period toggle
        periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentSymbol.isNotEmpty()) {
                currentPeriod = when (checkedId) {
                    R.id.btn1D -> "1d"
                    R.id.btn1W -> "1w"
                    R.id.btn1M -> "1m"
                    else -> "1d"
                }
                loadPriceHistory()
            }
        }

        // Buy button
        btnBuy.setOnClickListener { executeTrade(isBuy = true) }
        
        // Sell button
        btnSell.setOnClickListener { executeTrade(isBuy = false) }
    }

    private fun setQuantityPercentage(percentage: Float) {
        if (currentPrice <= 0) return
        val maxQuantity = (availableCash / currentPrice) * percentage
        etQuantity.setText(String.format("%.0f", maxQuantity.coerceAtLeast(1f)))
    }

    private fun performSearch(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val results = RetrofitClient.instance.searchStocks(query)
                withContext(Dispatchers.Main) {
                    if (results.isNotEmpty()) {
                        suggestionAdapter.updateData(results)
                        suggestionsCard.visibility = View.VISIBLE
                    } else {
                        hideSuggestions()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { hideSuggestions() }
            }
        }
    }

    private fun hideSuggestions() {
        suggestionsCard.visibility = View.GONE
        suggestionAdapter.clear()
    }

    private fun onStockSelected(stock: StockSearchResult) {
        hideSuggestions()
        
        baseSymbol = stock.symbol
        stockName = stock.name
        isIndianStock = stock.exchange == "NSE/BSE"
        
        // Update header
        tvHeaderTitle.text = stock.name
        
        // Hide search, show stock details
        searchSection.visibility = View.GONE
        stockDetailsSection.visibility = View.VISIBLE
        
        // Show exchange toggle for Indian stocks
        if (isIndianStock) {
            exchangeToggle.visibility = View.VISIBLE
            exchangeToggle.check(R.id.btnNse)
            currentExchange = "NSE"
        } else {
            exchangeToggle.visibility = View.GONE
        }
        
        // Set default period
        periodToggle.check(R.id.btn1D)
        currentPeriod = "1d"
        
        // Load stock data
        val fullSymbol = when {
            isIndianStock -> "${stock.symbol}.NS"
            stock.exchange == "Crypto" -> stock.symbol
            else -> stock.symbol
        }
        
        loadStockData(fullSymbol)
        loadHoldings()
    }

    private fun loadStockData(symbol: String) {
        currentSymbol = symbol
        loadPriceHistory()
    }

    private fun loadPriceHistory() {
        chartLoading.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val history = RetrofitClient.instance.getPriceHistory(currentSymbol, currentPeriod)
                
                withContext(Dispatchers.Main) {
                    chartLoading.visibility = View.GONE
                    updatePriceDisplay(history)
                    updateChart(history)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chartLoading.visibility = View.GONE
                    Toast.makeText(this@BuyAssetActivity, "Failed to load price data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePriceDisplay(history: PriceHistoryResponse) {
        currentPrice = history.current_price
        tvCurrentPrice.text = rupeeFormat.format(currentPrice)
        
        val changeSign = if (history.price_change >= 0) "+" else ""
        val changeColor = if (history.price_change >= 0) "#4CAF50" else "#F44336"
        tvPriceChange.text = "$changeSign${rupeeFormat.format(history.price_change)} ($changeSign${String.format("%.2f", history.price_change_percent)}%)"
        tvPriceChange.setTextColor(Color.parseColor(changeColor))
        
        calculateEstimatedCost()
    }

    private fun updateChart(history: PriceHistoryResponse) {
        val entries = history.data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.price)
        }
        
        val lineColor = if (history.price_change >= 0) "#4CAF50" else "#F44336"
        
        val dataSet = LineDataSet(entries, "Price").apply {
            color = Color.parseColor(lineColor)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor(lineColor)
            fillAlpha = 30
        }
        
        priceChart.data = LineData(dataSet)
        priceChart.invalidate()
    }

    private fun loadAvailableCash() {
        val email = userEmail ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolio = RetrofitClient.instance.getPortfolio(email)
                withContext(Dispatchers.Main) {
                    availableCash = portfolio.virtual_cash
                    tvAvailableCash.text = rupeeFormat.format(availableCash)
                }
            } catch (e: Exception) {
                // Use default
            }
        }
    }

    private fun loadHoldings() {
        val email = userEmail ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val holdings = RetrofitClient.instance.getHoldings(email)
                val holding = holdings.find { it.asset_symbol == currentSymbol }
                
                withContext(Dispatchers.Main) {
                    if (holding != null) {
                        currentHoldings = holding.quantity
                        avgBuyPrice = holding.average_buy_price
                        
                        tvHoldings.text = "${holding.quantity} shares"
                        holdingStatsRow.visibility = View.VISIBLE
                        tvAvgBuyPrice.text = rupeeFormat.format(holding.average_buy_price)
                        
                        val pnl = (currentPrice - holding.average_buy_price) * holding.quantity
                        val pnlSign = if (pnl >= 0) "+" else ""
                        tvUnrealizedPnl.text = "$pnlSign${rupeeFormat.format(pnl)}"
                        tvUnrealizedPnl.setTextColor(Color.parseColor(if (pnl >= 0) "#4CAF50" else "#F44336"))
                        
                        btnSell.isEnabled = true
                    } else {
                        tvHoldings.text = "0 shares"
                        holdingStatsRow.visibility = View.GONE
                        btnSell.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvHoldings.text = "0 shares"
                    btnSell.isEnabled = false
                }
            }
        }
    }

    private fun calculateEstimatedCost() {
        val quantity = etQuantity.text.toString().toFloatOrNull() ?: 0f
        val cost = currentPrice * quantity
        tvEstimatedCost.text = rupeeFormat.format(cost)
        
        btnBuy.isEnabled = quantity > 0 && cost <= availableCash && currentPrice > 0
    }

    private fun executeTrade(isBuy: Boolean) {
        val email = userEmail ?: return
        val quantity = etQuantity.text.toString().toFloatOrNull() ?: return
        
        if (quantity <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isBuy) {
            val cost = currentPrice * quantity
            if (cost > availableCash) {
                Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (quantity > currentHoldings) {
                Toast.makeText(this, "Cannot sell more than you own", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        loadingOverlay.visibility = View.VISIBLE
        
        val tradeRequest = TradeRequest(asset_symbol = currentSymbol, quantity = quantity)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.executeBuyOrder(tradeRequest, email)
                
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    val action = if (isBuy) "Purchased" else "Sold"
                    Toast.makeText(
                        this@BuyAssetActivity,
                        "✅ $action ${response.quantity} shares at ${rupeeFormat.format(response.executed_price)}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Refresh data
                    loadAvailableCash()
                    loadHoldings()
                    etQuantity.setText("1")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@BuyAssetActivity, "Trade failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
    }
}
