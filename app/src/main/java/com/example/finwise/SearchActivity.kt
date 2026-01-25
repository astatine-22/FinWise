package com.example.finwise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.StockSearchResult
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var etSearch: EditText
    private lateinit var btnClear: ImageView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var trendingContent: NestedScrollView
    private lateinit var emptyResults: LinearLayout

    // Section RecyclerViews
    private lateinit var rvIndianStocks: RecyclerView
    private lateinit var rvUsStocks: RecyclerView
    private lateinit var rvCrypto: RecyclerView

    // Shimmer Loading
    private lateinit var shimmerIndianStocks: ShimmerFrameLayout
    private lateinit var shimmerUsStocks: ShimmerFrameLayout
    private lateinit var shimmerCrypto: ShimmerFrameLayout

    // Adapters
    private lateinit var searchResultsAdapter: SearchResultAdapter
    private lateinit var indianStocksAdapter: TrendingStockAdapter
    private lateinit var usStocksAdapter: TrendingStockAdapter
    private lateinit var cryptoAdapter: TrendingCryptoAdapter

    // Search debounce
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DELAY_MS = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Handle status bar insets to prevent overlap
        val searchHeader = findViewById<LinearLayout>(R.id.searchHeader)
        ViewCompat.setOnApplyWindowInsetsListener(searchHeader) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = insets.top + 16) // 16dp base padding + status bar height
            windowInsets
        }

        initializeViews()
        setupAdapters()
        setupListeners()
        loadData()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)
        btnClear = findViewById(R.id.btnClear)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        trendingContent = findViewById(R.id.trendingContent)
        emptyResults = findViewById(R.id.emptyResults)

        // Sections
        rvIndianStocks = findViewById(R.id.rvIndianStocks)
        rvUsStocks = findViewById(R.id.rvUsStocks)
        rvCrypto = findViewById(R.id.rvCrypto)

        // Shimmers
        shimmerIndianStocks = findViewById(R.id.shimmerIndianStocks)
        shimmerUsStocks = findViewById(R.id.shimmerUsStocks)
        shimmerCrypto = findViewById(R.id.shimmerCrypto)
    }

    private fun setupAdapters() {
        // Search Results (Vertical)
        searchResultsAdapter = SearchResultAdapter(emptyList()) { stock ->
            openStockDetail(stock.symbol, stock.exchange ?: "NSE/BSE")
        }
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchResultsAdapter

        // Indian Stocks (Horizontal)
        indianStocksAdapter = TrendingStockAdapter(emptyList()) { stock ->
            openStockDetail(stock.symbol, "NSE/BSE")
        }
        rvIndianStocks.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvIndianStocks.adapter = indianStocksAdapter

        // US Stocks (Horizontal)
        usStocksAdapter = TrendingStockAdapter(emptyList()) { stock ->
            openStockDetail(stock.symbol, "US")
        }
        rvUsStocks.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvUsStocks.adapter = usStocksAdapter

        // Crypto (Horizontal)
        cryptoAdapter = TrendingCryptoAdapter(emptyList()) { crypto ->
            openStockDetail(crypto.symbol, "Crypto")
        }
        rvCrypto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCrypto.adapter = cryptoAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnClear.setOnClickListener {
            etSearch.setText("")
            btnClear.visibility = View.GONE
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                
                btnClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                if (query.length >= 2) {
                    searchRunnable = Runnable { performSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DELAY_MS)
                } else {
                    showTrendingContent()
                }
            }
        })
    }

    private fun loadData() {
        shimmerIndianStocks.startShimmer()
        shimmerUsStocks.startShimmer()
        shimmerCrypto.startShimmer()

        // 1. Indian Stocks (Top Gainers)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTopGainers()
                withContext(Dispatchers.Main) {
                    shimmerIndianStocks.stopShimmer()
                    shimmerIndianStocks.visibility = View.GONE
                    indianStocksAdapter.updateData(response.stocks.take(10))
                    rvIndianStocks.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    shimmerIndianStocks.stopShimmer()
                    shimmerIndianStocks.visibility = View.GONE
                }
            }
        }

        // 2. US Stocks
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getUsStocks()
                withContext(Dispatchers.Main) {
                    shimmerUsStocks.stopShimmer()
                    shimmerUsStocks.visibility = View.GONE
                    usStocksAdapter.updateData(response.stocks.take(10))
                    rvUsStocks.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    shimmerUsStocks.stopShimmer()
                    shimmerUsStocks.visibility = View.GONE
                }
            }
        }

        // 3. Crypto
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getCryptoList()
                withContext(Dispatchers.Main) {
                    shimmerCrypto.stopShimmer()
                    shimmerCrypto.visibility = View.GONE
                    cryptoAdapter.updateData(response.cryptos.take(10))
                    rvCrypto.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    shimmerCrypto.stopShimmer()
                    shimmerCrypto.visibility = View.GONE
                }
            }
        }
    }

    private fun performSearch(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val results = RetrofitClient.instance.searchStocks(query)
                withContext(Dispatchers.Main) {
                    if (results.isNotEmpty()) {
                        showSearchResults(results)
                    } else {
                        showEmptyResults()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmptyResults()
                }
            }
        }
    }

    private fun showSearchResults(results: List<StockSearchResult>) {
        trendingContent.visibility = View.GONE
        emptyResults.visibility = View.GONE
        rvSearchResults.visibility = View.VISIBLE
        searchResultsAdapter.updateData(results)
    }

    private fun showTrendingContent() {
        rvSearchResults.visibility = View.GONE
        emptyResults.visibility = View.GONE
        trendingContent.visibility = View.VISIBLE
        searchResultsAdapter.clear()
    }

    private fun showEmptyResults() {
        rvSearchResults.visibility = View.GONE
        trendingContent.visibility = View.GONE
        emptyResults.visibility = View.VISIBLE
    }

    private fun openStockDetail(symbol: String, exchange: String) {
        val intent = Intent(this, BuyAssetActivity::class.java)
        // Ensure symbol handling is consistent
        val fullSymbol = when {
            exchange.contains("NSE") || exchange.contains("BSE") -> 
                if (symbol.endsWith(".NS") || symbol.endsWith(".BO")) symbol else "$symbol.NS"
            exchange == "Crypto" -> symbol
            else -> symbol
        }
        intent.putExtra("PREFILLED_SYMBOL", fullSymbol)
        intent.putExtra("USER_EMAIL", getSharedPreferences("FinWise", MODE_PRIVATE).getString("user_email", ""))
        startActivity(intent)
    }
}
