package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.RetrofitClient
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class UsMarketFragment : Fragment() {

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

    private lateinit var usStockAdapter: UsStockCardAdapter
    private lateinit var usGainersAdapter: UsHorizontalStockAdapter
    private lateinit var usLosersAdapter: UsHorizontalStockAdapter

    private var userEmail: String? = null
    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_us_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupAdapters()
        setupListeners()

        // Load US stocks dashboard
        loadUsStocksDashboard()
    }

    override fun onResume() {
        super.onResume()
        // Reload dashboard when fragment becomes visible again
        loadUsStocksDashboard()
    }

    private fun initializeViews(view: View) {
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
    }

    private fun setupAdapters() {
        // US Stocks adapter (price in $) - Vertical list
        usStockAdapter = UsStockCardAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)
        }
        rvUsStocks.layoutManager = LinearLayoutManager(context)
        rvUsStocks.adapter = usStockAdapter

        // US Gainers adapter (horizontal)
        usGainersAdapter = UsHorizontalStockAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)
        }
        rvUsGainers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvUsGainers.adapter = usGainersAdapter

        // Fix horizontal scroll conflict for gainers
        rvUsGainers.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> rv.parent.requestDisallowInterceptTouchEvent(true)
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        // US Losers adapter (horizontal)
        usLosersAdapter = UsHorizontalStockAdapter(emptyList()) { stock ->
            openBuyActivity(stock.symbol)
        }
        rvUsLosers.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvUsLosers.adapter = usLosersAdapter

        // Fix horizontal scroll conflict for losers
        rvUsLosers.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> rv.parent.requestDisallowInterceptTouchEvent(true)
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun setupListeners() {
        btnUsInvestNow.setOnClickListener { openBuyActivity(null) }
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

    private fun openBuyActivity(prefilledSymbol: String?) {
        val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
            prefilledSymbol?.let { putExtra("PREFILLED_SYMBOL", it) }
        }
        startActivity(intent)
    }
}
