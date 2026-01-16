package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.MarketIndex
import com.example.finwise.api.RetrofitClient
import com.example.finwise.api.StockItem
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TradeFragment : Fragment() {

    // UI Components - Tabs
    private lateinit var tabLayout: TabLayout
    private lateinit var stocksTabContent: LinearLayout      // Tab 0: Indian Stocks
    private lateinit var usStocksTabContent: LinearLayout    // Tab 1: US Stocks
    private lateinit var cryptoTabContent: LinearLayout      // Tab 2: Crypto
    private lateinit var portfolioTabContent: LinearLayout   // Tab 3: Portfolio

    // Indian Stocks Tab
    private lateinit var tvWalletBalance: TextView
    private lateinit var btnInvestNow: TextView
    private lateinit var marketIndicesLayout: LinearLayout
    private lateinit var chipExplore: TextView
    private lateinit var chipTopGainers: TextView
    private lateinit var chipTopLosers: TextView
    private lateinit var rvStocks: RecyclerView
    private lateinit var shimmerStocks: ShimmerFrameLayout

    // US Stocks Tab (Full Dashboard)
    private lateinit var tvUsPortfolioValue: TextView
    private lateinit var btnUsInvestNow: TextView
    private lateinit var tvUsHoldingsCount: TextView
    private lateinit var tvUsExchangeRate: TextView
    private lateinit var tvUsGainersCount: TextView
    private lateinit var tvUsLosersCount: TextView
    private lateinit var rvUsGainers: RecyclerView
    private lateinit var rvUsLosers: RecyclerView
    private lateinit var rvUsStocks: RecyclerView
    private lateinit var shimmerUsStocks: ShimmerFrameLayout

    // Crypto Tab
    private lateinit var rvCrypto: RecyclerView
    private lateinit var shimmerCrypto: ShimmerFrameLayout

    // Portfolio Tab
    private lateinit var tvTotalValue: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvInvestedValue: TextView
    private lateinit var tvHoldingsCount: TextView
    private lateinit var rvHoldings: RecyclerView
    private lateinit var emptyHoldingsLayout: LinearLayout
    private lateinit var btnBuyAsset: MaterialButton
    private lateinit var btnResetPortfolio: MaterialButton

    // FAB
    private lateinit var fabQuickBuy: FloatingActionButton

    // General
    private lateinit var loadingOverlay: FrameLayout

    // Adapters
    private lateinit var indianStockAdapter: StockCardAdapter
    private lateinit var usStockAdapter: UsStockCardAdapter
    private lateinit var usGainersAdapter: UsHorizontalStockAdapter
    private lateinit var usLosersAdapter: UsHorizontalStockAdapter
    private lateinit var cryptoCardAdapter: CryptoCardAdapter
    private lateinit var holdingsAdapter: HoldingsAdapter

    private var userEmail: String? = null
    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dollarFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private var currentCategory = "explore"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupAdapters()
        setupListeners()

        // Load initial data for Tab 0 (Indian Stocks)
        loadMarketIndices()
        loadIndianStocks("explore")
        loadWalletBalance()
    }

    override fun onResume() {
        super.onResume()
        if (::tabLayout.isInitialized) {
            when (tabLayout.selectedTabPosition) {
                0 -> loadWalletBalance()
                1 -> loadUsStocksDashboard()
                3 -> loadPortfolio()
            }
        }
    }

    private fun initializeViews(view: View) {
        // Tabs
        tabLayout = view.findViewById(R.id.tabLayout)
        stocksTabContent = view.findViewById(R.id.stocksTabContent)
        usStocksTabContent = view.findViewById(R.id.usStocksTabContent)
        cryptoTabContent = view.findViewById(R.id.cryptoTabContent)
        portfolioTabContent = view.findViewById(R.id.portfolioTabContent)

        // Indian Stocks Tab
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance)
        btnInvestNow = view.findViewById(R.id.btnInvestNow)
        marketIndicesLayout = view.findViewById(R.id.marketIndicesLayout)
        chipExplore = view.findViewById(R.id.chipExplore)
        chipTopGainers = view.findViewById(R.id.chipTopGainers)
        chipTopLosers = view.findViewById(R.id.chipTopLosers)
        rvStocks = view.findViewById(R.id.rvStocks)
        shimmerStocks = view.findViewById(R.id.shimmerStocks)

        // US Stocks Tab (Full Dashboard)
        tvUsPortfolioValue = view.findViewById(R.id.tvUsPortfolioValue)
        btnUsInvestNow = view.findViewById(R.id.btnUsInvestNow)
        tvUsHoldingsCount = view.findViewById(R.id.tvUsHoldingsCount)
        tvUsExchangeRate = view.findViewById(R.id.tvUsExchangeRate)
        tvUsGainersCount = view.findViewById(R.id.tvUsGainersCount)
        tvUsLosersCount = view.findViewById(R.id.tvUsLosersCount)
        rvUsGainers = view.findViewById(R.id.rvUsGainers)
        rvUsLosers = view.findViewById(R.id.rvUsLosers)
        rvUsStocks = view.findViewById(R.id.rvUsStocks)
        shimmerUsStocks = view.findViewById(R.id.shimmerUsStocks)

        // Crypto Tab
        rvCrypto = view.findViewById(R.id.rvCrypto)
        shimmerCrypto = view.findViewById(R.id.shimmerCrypto)

        // Portfolio Tab
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        tvInvestedValue = view.findViewById(R.id.tvInvestedValue)
        tvHoldingsCount = view.findViewById(R.id.tvHoldingsCount)
        rvHoldings = view.findViewById(R.id.rvHoldings)
        emptyHoldingsLayout = view.findViewById(R.id.emptyHoldingsLayout)
        btnBuyAsset = view.findViewById(R.id.btnBuyAsset)
        btnResetPortfolio = view.findViewById(R.id.btnResetPortfolio)

        // FAB
        fabQuickBuy = view.findViewById(R.id.fabQuickBuy)

        // General
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
    }

    private fun setupAdapters() {
        // Indian Stocks adapter (price in ₹)
        indianStockAdapter = StockCardAdapter(emptyList()) { stock ->
            openBuyActivity("${stock.symbol}.NS")
        }
        rvStocks.layoutManager = LinearLayoutManager(context)
        rvStocks.adapter = indianStockAdapter

        // US Stocks adapter (price in $) - Vertical list
        usStockAdapter = UsStockCardAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)  // US stocks don't need suffix
        }
        rvUsStocks.layoutManager = LinearLayoutManager(context)
        rvUsStocks.adapter = usStockAdapter

        // US Gainers adapter (horizontal)
        usGainersAdapter = UsHorizontalStockAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)
        }
        rvUsGainers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvUsGainers.adapter = usGainersAdapter

        // US Losers adapter (horizontal)
        usLosersAdapter = UsHorizontalStockAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)
        }
        rvUsLosers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvUsLosers.adapter = usLosersAdapter

        // Crypto adapter
        cryptoCardAdapter = CryptoCardAdapter(emptyList()) { crypto ->
            openBuyActivity(crypto.symbol)
        }
        rvCrypto.layoutManager = LinearLayoutManager(context)
        rvCrypto.adapter = cryptoCardAdapter

        // Holdings adapter
        holdingsAdapter = HoldingsAdapter(emptyList(), rupeeFormat)
        rvHoldings.layoutManager = LinearLayoutManager(context)
        rvHoldings.adapter = holdingsAdapter
    }

    private fun setupListeners() {
        // Invest Now button (Indian)
        btnInvestNow.setOnClickListener { openBuyActivity(null) }

        // Invest Now button (US)
        btnUsInvestNow.setOnClickListener { openBuyActivity(null) }

        // FAB
        fabQuickBuy.setOnClickListener { openBuyActivity(null) }

        // Tab switching (4 tabs)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        showTab(stocksTabContent)
                        loadWalletBalance()
                    }
                    1 -> {
                        showTab(usStocksTabContent)
                        loadUsStocksDashboard()
                    }
                    2 -> {
                        showTab(cryptoTabContent)
                        loadCryptos()
                    }
                    3 -> {
                        showTab(portfolioTabContent)
                        loadPortfolio()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Category chips (TextViews) for Indian stocks
        chipExplore.setOnClickListener { 
            selectChip(chipExplore)
            loadIndianStocks("explore") 
        }
        chipTopGainers.setOnClickListener { 
            selectChip(chipTopGainers)
            loadIndianStocks("gainers") 
        }
        chipTopLosers.setOnClickListener { 
            selectChip(chipTopLosers)
            loadIndianStocks("losers") 
        }

        // Portfolio buttons
        btnBuyAsset.setOnClickListener { openBuyActivity(null) }
        btnResetPortfolio.setOnClickListener { showResetConfirmation() }
    }

    private fun selectChip(selected: TextView) {
        val context = context ?: return
        
        // Reset all chips
        listOf(chipExplore, chipTopGainers, chipTopLosers).forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }
    }

    private fun showTab(tabToShow: LinearLayout) {
        stocksTabContent.visibility = View.GONE
        usStocksTabContent.visibility = View.GONE
        cryptoTabContent.visibility = View.GONE
        portfolioTabContent.visibility = View.GONE
        tabToShow.visibility = View.VISIBLE
    }

    private fun showStocksLoading(show: Boolean) {
        if (show) {
            shimmerStocks.visibility = View.VISIBLE
            shimmerStocks.startShimmer()
            rvStocks.visibility = View.GONE
        } else {
            shimmerStocks.stopShimmer()
            shimmerStocks.visibility = View.GONE
            rvStocks.visibility = View.VISIBLE
        }
    }

    private fun showUsStocksLoading(show: Boolean) {
        if (show) {
            shimmerUsStocks.visibility = View.VISIBLE
            shimmerUsStocks.startShimmer()
            rvUsStocks.visibility = View.GONE
        } else {
            shimmerUsStocks.stopShimmer()
            shimmerUsStocks.visibility = View.GONE
            rvUsStocks.visibility = View.VISIBLE
        }
    }

    private fun showCryptoLoading(show: Boolean) {
        if (show) {
            shimmerCrypto.visibility = View.VISIBLE
            shimmerCrypto.startShimmer()
            rvCrypto.visibility = View.GONE
        } else {
            shimmerCrypto.stopShimmer()
            shimmerCrypto.visibility = View.GONE
            rvCrypto.visibility = View.VISIBLE
        }
    }

    private fun loadWalletBalance() {
        val email = userEmail ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getPortfolio(email)
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    tvWalletBalance.text = rupeeFormat.format(response.virtual_cash)
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun loadMarketIndices() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getMarketIndices()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    displayMarketIndices(response.indices)
                }
            } catch (e: Exception) {
                // Silently fail for indices
            }
        }
    }

    private fun displayMarketIndices(indices: List<MarketIndex>) {
        marketIndicesLayout.removeAllViews()

        indices.forEach { index ->
            val indexView = layoutInflater.inflate(R.layout.item_market_index, marketIndicesLayout, false)

            indexView.findViewById<TextView>(R.id.tvIndexName).text = index.name
            indexView.findViewById<TextView>(R.id.tvIndexValue).text = 
                NumberFormat.getNumberInstance(Locale("en", "IN")).format(index.value)

            val changeView = indexView.findViewById<TextView>(R.id.tvIndexChange)
            val sign = if (index.is_positive) "+" else "-"
            changeView.text = "$sign${String.format("%.2f", kotlin.math.abs(index.change_percent))}%"
            
            // Set color based on positive/negative
            val colorRes = if (index.is_positive) R.color.finwise_green else R.color.red_error
            changeView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

            marketIndicesLayout.addView(indexView)
        }
    }

    /**
     * Load Indian Stocks (Tab 0)
     */
    private fun loadIndianStocks(category: String) {
        currentCategory = category
        showStocksLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = when (category) {
                    "gainers" -> RetrofitClient.instance.getTopGainers()
                    "losers" -> RetrofitClient.instance.getTopLosers()
                    else -> RetrofitClient.instance.getIndianStocks()
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showStocksLoading(false)
                    indianStockAdapter.updateData(response.stocks)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showStocksLoading(false)
                    Toast.makeText(context, "Failed to load Indian stocks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Load Full US Stocks Dashboard (Tab 1)
     * - Fetches all US stocks
     * - Client-side sorts into Gainers and Losers
     * - Calculates US portfolio value from holdings
     */
    private fun loadUsStocksDashboard() {
        showUsStocksLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch US stocks and crypto data in parallel to get the live exchange rate
                val usStocksDeferred = async { RetrofitClient.instance.getUsStocks() }
                val cryptoDeferred = async { RetrofitClient.instance.getCryptoList() }
                
                val usStocksResponse = usStocksDeferred.await()
                val cryptoResponse = cryptoDeferred.await()
                
                // Extract live USD to INR rate
                val liveExchangeRate = cryptoResponse.usd_to_inr
                
                val email = userEmail
                var usHoldingsValue = 0.0
                var usHoldingsCount = 0
                
                // Calculate US holdings value if user is logged in
                if (email != null) {
                    try {
                        val portfolio = RetrofitClient.instance.getPortfolio(email)
                        // Filter holdings that are US stocks (no .NS, .BO suffix)
                        portfolio.holdings.forEach { holding ->
                            val isUsStock = !holding.asset_symbol.endsWith(".NS") && 
                                           !holding.asset_symbol.endsWith(".BO") &&
                                           !holding.asset_symbol.contains("-USD") &&
                                           !holding.asset_symbol.contains("-INR")
                            if (isUsStock && holding.current_value != null) {
                                usHoldingsValue += holding.current_value
                                usHoldingsCount++
                            }
                        }
                    } catch (e: Exception) {
                        // Silently fail portfolio fetch
                    }
                }

                val allUsStocks = usStocksResponse.stocks
                
                // Client-side sorting: Gainers and Losers
                val gainers = allUsStocks
                    .filter { it.is_positive && it.change_percent > 0 }
                    .sortedByDescending { it.change_percent }
                    .take(5)
                
                val losers = allUsStocks
                    .filter { !it.is_positive && it.change_percent < 0 }
                    .sortedBy { it.change_percent }
                    .take(5)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showUsStocksLoading(false)
                    
                    // Update portfolio card
                    tvUsPortfolioValue.text = rupeeFormat.format(usHoldingsValue)
                    tvUsHoldingsCount.text = "$usHoldingsCount stocks"
                    tvUsExchangeRate.text = "$1 = ₹${String.format("%.2f", liveExchangeRate)}"
                    
                    // Update gainers/losers counts
                    if (gainers.isNotEmpty()) {
                        tvUsGainersCount.text = "${gainers.size} stocks"
                    }
                    if (losers.isNotEmpty()) {
                        tvUsLosersCount.text = "${losers.size} stocks"
                    }
                    
                    // Update adapters
                    usGainersAdapter.updateData(gainers)
                    usLosersAdapter.updateData(losers)
                    usStockAdapter.updateData(allUsStocks)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showUsStocksLoading(false)
                    Toast.makeText(context, "Failed to load US stocks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Load Cryptos (Tab 2)
     */
    private fun loadCryptos() {
        showCryptoLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getCryptoList()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showCryptoLoading(false)
                    cryptoCardAdapter.updateData(response.cryptos)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    showCryptoLoading(false)
                    Toast.makeText(context, "Failed to load cryptos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Load Portfolio (Tab 3)
     */
    private fun loadPortfolio() {
        val email = userEmail ?: return
        loadingOverlay.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolio = RetrofitClient.instance.getPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    displayPortfolio(portfolio)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Failed to load portfolio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayPortfolio(portfolio: com.example.finwise.api.PortfolioSummaryResponse) {
        tvTotalValue.text = rupeeFormat.format(portfolio.total_portfolio_value)
        tvCashBalance.text = rupeeFormat.format(portfolio.virtual_cash)
        tvInvestedValue.text = rupeeFormat.format(portfolio.total_holdings_value)

        val holdings = portfolio.holdings
        tvHoldingsCount.text = "${holdings.size} assets"

        if (holdings.isEmpty()) {
            emptyHoldingsLayout.visibility = View.VISIBLE
            rvHoldings.visibility = View.GONE
        } else {
            emptyHoldingsLayout.visibility = View.GONE
            rvHoldings.visibility = View.VISIBLE
            holdingsAdapter.updateData(holdings)
        }
    }

    private fun openBuyActivity(prefilledSymbol: String?) {
        val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
            prefilledSymbol?.let { putExtra("PREFILLED_SYMBOL", it) }
        }
        startActivity(intent)
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Portfolio")
            .setMessage("Are you sure you want to reset your portfolio? This will:\n\n• Delete all your holdings\n• Reset cash to ₹1,00,000\n\nThis cannot be undone.")
            .setPositiveButton("Reset") { dialog, _ ->
                resetPortfolio()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun resetPortfolio() {
        val email = userEmail ?: return
        loadingOverlay.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.resetPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Portfolio reset successfully!", Toast.LENGTH_SHORT).show()
                    loadPortfolio()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Failed to reset portfolio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
