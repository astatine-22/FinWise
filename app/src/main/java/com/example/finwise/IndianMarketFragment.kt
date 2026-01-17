package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.MarketIndex
import com.example.finwise.api.RetrofitClient
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class IndianMarketFragment : Fragment() {

    private lateinit var tvWalletBalance: TextView
    private lateinit var btnInvestNow: TextView
    private lateinit var marketIndicesLayout: LinearLayout
    private lateinit var chipExplore: TextView
    private lateinit var chipTopGainers: TextView
    private lateinit var chipTopLosers: TextView
    private lateinit var rvStocks: RecyclerView
    private lateinit var shimmerStocks: ShimmerFrameLayout

    private lateinit var indianStockAdapter: StockCardAdapter
    private var userEmail: String? = null
    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var currentCategory = "explore"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_indian_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupAdapter()
        setupListeners()

        // Load initial data
        loadWalletBalance()
        loadMarketIndices()
        loadIndianStocks("explore")
    }

    override fun onResume() {
        super.onResume()
        // Reload wallet balance when fragment becomes visible again
        loadWalletBalance()
    }

    private fun initializeViews(view: View) {
        tvWalletBalance = view.findViewById(R.id.tvWalletBalance)
        btnInvestNow = view.findViewById(R.id.btnInvestNow)
        marketIndicesLayout = view.findViewById(R.id.marketIndicesLayout)
        chipExplore = view.findViewById(R.id.chipExplore)
        chipTopGainers = view.findViewById(R.id.chipTopGainers)
        chipTopLosers = view.findViewById(R.id.chipTopLosers)
        rvStocks = view.findViewById(R.id.rvStocks)
        shimmerStocks = view.findViewById(R.id.shimmerStocks)
    }

    private fun setupAdapter() {
        indianStockAdapter = StockCardAdapter(emptyList()) { stock ->
            openBuyActivity("${stock.symbol}.NS")
        }
        rvStocks.layoutManager = LinearLayoutManager(context)
        rvStocks.adapter = indianStockAdapter
    }

    private fun setupListeners() {
        btnInvestNow.setOnClickListener { openBuyActivity(null) }

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
    }

    private fun selectChip(selected: TextView) {
        val context = context ?: return

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

    private fun openBuyActivity(prefilledSymbol: String?) {
        val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
            prefilledSymbol?.let { putExtra("PREFILLED_SYMBOL", it) }
        }
        startActivity(intent)
    }
}
