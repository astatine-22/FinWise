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
import androidx.core.content.ContextCompat
import com.example.finwise.api.PriceHistoryResponse
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.StockSearchResult
import com.example.finwise.api.TradeRequest
import com.example.finwise.api.SellRequest
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class BuyAssetActivity : AppCompatActivity() {

    // UI Components - Header
    private lateinit var btnBack: ImageView
    private lateinit var tvStockSymbol: TextView
    private lateinit var tvStockName: TextView
    private lateinit var tvCurrentPrice: TextView
    private lateinit var tvPriceChange: TextView
    private lateinit var tvDayHigh: TextView
    private lateinit var tvDayLow: TextView

    // UI Components - Search
    private lateinit var searchSection: LinearLayout
    private lateinit var stockDetailsSection: LinearLayout
    private lateinit var etSymbol: EditText
    private lateinit var suggestionsCard: androidx.cardview.widget.CardView
    private lateinit var rvSuggestions: androidx.recyclerview.widget.RecyclerView
    private lateinit var suggestionAdapter: StockSuggestionAdapter

    // UI Components - Chart
    private lateinit var priceChart: LineChart
    private lateinit var chartLoading: ProgressBar
    private lateinit var btn1D: TextView
    private lateinit var btn1W: TextView
    private lateinit var btn1M: TextView

    // UI Components - Tabs
    private lateinit var tabBuy: TextView
    private lateinit var tabSell: TextView

    // UI Components - Info Cards
    private lateinit var tvAvailableCash: TextView
    private lateinit var tvHoldings: TextView

    // UI Components - Quantity
    private lateinit var tvQuantityLabel: TextView
    private lateinit var btnMinus: MaterialButton
    private lateinit var btnPlus: MaterialButton
    private lateinit var etQuantity: EditText
    private lateinit var tvMarketPriceHelper: TextView
    private lateinit var chip25: TextView
    private lateinit var chip50: TextView
    private lateinit var chipMax: TextView

    // UI Components - Exchange Toggle
    private lateinit var exchangeToggleContainer: LinearLayout
    private lateinit var btnNse: TextView
    private lateinit var btnBse: TextView

    // UI Components - Bottom
    private lateinit var tvEstimatedCost: TextView
    private lateinit var btnAction: MaterialButton
    private lateinit var loadingOverlay: FrameLayout

    // State
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
    private var isBuyMode: Boolean = true

    // Search debounce
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
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
        // Header
        btnBack = findViewById(R.id.btnBack)
        tvStockSymbol = findViewById(R.id.tvStockSymbol)
        tvStockName = findViewById(R.id.tvStockName)
        tvCurrentPrice = findViewById(R.id.tvCurrentPrice)
        tvPriceChange = findViewById(R.id.tvPriceChange)
        tvDayHigh = findViewById(R.id.tvDayHigh)
        tvDayLow = findViewById(R.id.tvDayLow)

        // Search Section
        searchSection = findViewById(R.id.searchSection)
        stockDetailsSection = findViewById(R.id.stockDetailsSection)
        etSymbol = findViewById(R.id.etSymbol)
        suggestionsCard = findViewById(R.id.suggestionsCard)
        rvSuggestions = findViewById(R.id.rvSuggestions)
        
        // Setup suggestions RecyclerView
        suggestionAdapter = StockSuggestionAdapter(emptyList()) { stock ->
            onStockSelected(stock)
        }
        rvSuggestions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvSuggestions.adapter = suggestionAdapter

        // Chart
        priceChart = findViewById(R.id.priceChart)
        chartLoading = findViewById(R.id.chartLoading)
        btn1D = findViewById(R.id.btn1D)
        btn1W = findViewById(R.id.btn1W)
        btn1M = findViewById(R.id.btn1M)

        // Tabs
        tabBuy = findViewById(R.id.tabBuy)
        tabSell = findViewById(R.id.tabSell)

        // Info Cards
        tvAvailableCash = findViewById(R.id.tvAvailableCash)
        tvHoldings = findViewById(R.id.tvHoldings)

        // Quantity
        tvQuantityLabel = findViewById(R.id.tvQuantityLabel)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        etQuantity = findViewById(R.id.etQuantity)
        tvMarketPriceHelper = findViewById(R.id.tvMarketPriceHelper)
        chip25 = findViewById(R.id.chip25)
        chip50 = findViewById(R.id.chip50)
        chipMax = findViewById(R.id.chipMax)

        // Exchange
        exchangeToggleContainer = findViewById(R.id.exchangeToggleContainer)
        btnNse = findViewById(R.id.btnNse)
        btnBse = findViewById(R.id.btnBse)

        // Bottom
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost)
        btnAction = findViewById(R.id.btnAction)
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
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#9CA3AF")
                textSize = 10f
            }
            
            axisRight.isEnabled = false
            
            setNoDataText("Loading chart...")
            setNoDataTextColor(Color.parseColor("#9CA3AF"))
        }
    }

    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener { finish() }

        // Search text with debounce
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

        // BUY/SELL Tabs
        tabBuy.setOnClickListener { switchToMode(isBuy = true) }
        tabSell.setOnClickListener { switchToMode(isBuy = false) }

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

        // Exchange toggle (NSE/BSE)
        btnNse.setOnClickListener { switchExchange("NSE") }
        btnBse.setOnClickListener { switchExchange("BSE") }

        // Period tabs
        btn1D.setOnClickListener { switchPeriod("1d", btn1D) }
        btn1W.setOnClickListener { switchPeriod("1w", btn1W) }
        btn1M.setOnClickListener { switchPeriod("1m", btn1M) }

        // Action button
        btnAction.setOnClickListener { executeTrade() }
    }

    private fun switchToMode(isBuy: Boolean) {
        isBuyMode = isBuy
        
        if (isBuy) {
            tabBuy.setBackgroundResource(R.drawable.bg_tab_selected_green)
            tabBuy.setTextColor(Color.WHITE)
            tabSell.setBackgroundResource(0)
            tabSell.setTextColor(Color.parseColor("#6B7280"))
            
            btnAction.setBackgroundColor(Color.parseColor("#22C55E"))
            btnAction.text = "BUY ${stockName.uppercase()}"
            
            tvQuantityLabel.text = "Number of Shares"
            btnMinus.setTextColor(Color.parseColor("#22C55E"))
            btnMinus.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E"))
            btnPlus.setTextColor(Color.parseColor("#22C55E"))
            btnPlus.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E"))
        } else {
            tabSell.setBackgroundResource(R.drawable.bg_tab_selected_red)
            tabSell.setTextColor(Color.WHITE)
            tabBuy.setBackgroundResource(0)
            tabBuy.setTextColor(Color.parseColor("#6B7280"))
            
            btnAction.setBackgroundColor(Color.parseColor("#EF4444"))
            btnAction.text = "SELL ${stockName.uppercase()}"
            
            tvQuantityLabel.text = "Shares to Sell"
            btnMinus.setTextColor(Color.parseColor("#EF4444"))
            btnMinus.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
            btnPlus.setTextColor(Color.parseColor("#EF4444"))
            btnPlus.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
        }
        
        calculateEstimatedCost()
    }

    private fun switchExchange(exchange: String) {
        if (exchange == currentExchange) return
        currentExchange = exchange
        
        if (exchange == "NSE") {
            btnNse.setBackgroundResource(R.drawable.bg_exchange_selected)
            btnNse.setTextColor(Color.WHITE)
            btnBse.setBackgroundResource(0)
            btnBse.setTextColor(Color.parseColor("#6B7280"))
        } else {
            btnBse.setBackgroundResource(R.drawable.bg_exchange_selected)
            btnBse.setTextColor(Color.WHITE)
            btnNse.setBackgroundResource(0)
            btnNse.setTextColor(Color.parseColor("#6B7280"))
        }
        
        val suffix = if (exchange == "NSE") ".NS" else ".BO"
        loadStockData("$baseSymbol$suffix")
    }

    private fun switchPeriod(period: String, selectedTab: TextView) {
        currentPeriod = period
        
        // Reset all
        listOf(btn1D, btn1W, btn1M).forEach {
            it.setBackgroundResource(0)
            it.setTextColor(Color.parseColor("#6B7280"))
        }
        
        // Highlight selected
        selectedTab.setBackgroundResource(R.drawable.bg_period_selected)
        selectedTab.setTextColor(Color.WHITE)
        
        if (currentSymbol.isNotEmpty()) {
            loadPriceHistory()
        }
    }

    private fun setQuantityPercentage(percentage: Float) {
        if (currentPrice <= 0) return
        
        val maxQuantity = if (isBuyMode) {
            (availableCash / currentPrice) * percentage
        } else {
            currentHoldings * percentage
        }
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
        // Hide search, show stock details
        hideSuggestions()
        etSymbol.clearFocus()
        searchSection.visibility = View.GONE
        stockDetailsSection.visibility = View.VISIBLE
        
        baseSymbol = stock.symbol
        stockName = stock.name
        isIndianStock = stock.exchange == "NSE/BSE"
        
        // Update header
        tvStockSymbol.text = stock.symbol.uppercase()
        tvStockName.text = stock.name
        
        // Show exchange toggle for Indian stocks
        if (isIndianStock) {
            exchangeToggleContainer.visibility = View.VISIBLE
            switchExchange("NSE")
        } else {
            exchangeToggleContainer.visibility = View.GONE
        }
        
        // Set default period
        switchPeriod("1d", btn1D)
        
        // Load stock data
        val fullSymbol = when {
            isIndianStock -> "${stock.symbol}.NS"
            stock.exchange == "Crypto" -> stock.symbol
            else -> stock.symbol
        }
        
        loadStockData(fullSymbol)
        loadHoldings()
        
        // Update action button
        btnAction.text = "BUY ${stockName.uppercase()}"
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
        tvPriceChange.text = "$changeSign${rupeeFormat.format(history.price_change)} ($changeSign${String.format("%.2f", history.price_change_percent)}%)"
        
        // Day High/Low from price data
        if (history.data.isNotEmpty()) {
            val prices = history.data.map { it.price }
            tvDayHigh.text = rupeeFormat.format(prices.maxOrNull() ?: currentPrice)
            tvDayLow.text = rupeeFormat.format(prices.minOrNull() ?: currentPrice)
        }
        
        tvMarketPriceHelper.text = "× Market Price ${rupeeFormat.format(currentPrice)}"
        
        calculateEstimatedCost()
    }

    private fun updateChart(history: PriceHistoryResponse) {
        val entries = history.data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.price)
        }
        
        val lineColor = if (history.price_change >= 0) "#22C55E" else "#EF4444"
        
        val dataSet = LineDataSet(entries, "Price").apply {
            color = Color.parseColor(lineColor)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor(lineColor)
            fillAlpha = 25
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
                        tvHoldings.text = "${holding.quantity.toInt()} shares"
                    } else {
                        currentHoldings = 0f
                        tvHoldings.text = "0 shares"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvHoldings.text = "0 shares"
                }
            }
        }
    }

    private fun calculateEstimatedCost() {
        val quantity = etQuantity.text.toString().toFloatOrNull() ?: 0f
        val cost = currentPrice * quantity
        tvEstimatedCost.text = rupeeFormat.format(cost)
        
        btnAction.isEnabled = if (isBuyMode) {
            quantity > 0 && cost <= availableCash && currentPrice > 0
        } else {
            quantity > 0 && quantity <= currentHoldings && currentPrice > 0
        }
    }

    private fun executeTrade() {
        val email = userEmail ?: return
        val quantity = etQuantity.text.toString().toFloatOrNull() ?: return
        
        if (quantity <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isBuyMode) {
            val cost = currentPrice * quantity
            if (cost > availableCash) {
                Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show()
                return
            }
            executeBuyOrder(email, quantity)
        } else {
            if (quantity > currentHoldings) {
                Toast.makeText(this, "Cannot sell more than you own", Toast.LENGTH_SHORT).show()
                return
            }
            executeSellOrder(email, quantity)
        }
    }

    private fun executeBuyOrder(email: String, quantity: Float) {
        loadingOverlay.visibility = View.VISIBLE
        
        val tradeRequest = TradeRequest(asset_symbol = currentSymbol, quantity = quantity)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.executeBuyOrder(tradeRequest, email)
                
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(
                        this@BuyAssetActivity,
                        "✅ Purchased ${response.quantity.toInt()} shares at ${rupeeFormat.format(response.executed_price)}",
                        Toast.LENGTH_LONG
                    ).show()
                    
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

    private fun executeSellOrder(email: String, quantity: Float) {
        loadingOverlay.visibility = View.VISIBLE
        
        val sellRequest = SellRequest(email = email, symbol = currentSymbol, quantity = quantity)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.executeSellOrder(sellRequest)
                
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(
                        this@BuyAssetActivity,
                        "✅ Sold ${response.quantity_sold.toInt()} shares at ${rupeeFormat.format(response.executed_price)}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    loadAvailableCash()
                    loadHoldings()
                    etQuantity.setText("1")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@BuyAssetActivity, "Sell failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
