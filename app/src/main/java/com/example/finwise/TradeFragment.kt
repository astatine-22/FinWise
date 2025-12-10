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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.HoldingResponse
import com.example.finwise.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TradeFragment : Fragment() {

    // UI Components
    private lateinit var tvTotalValue: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvInvestedValue: TextView
    private lateinit var tvHoldingsCount: TextView
    private lateinit var rvHoldings: RecyclerView
    private lateinit var emptyHoldingsLayout: LinearLayout
    private lateinit var popularStocksLayout: LinearLayout
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var btnBuyAsset: MaterialButton
    private lateinit var btnResetPortfolio: MaterialButton

    private lateinit var holdingsAdapter: HoldingsAdapter
    private var userEmail: String? = null

    // Popular Indian stocks for quick access
    private val popularStocks = listOf(
        "RELIANCE.NS" to "Reliance",
        "TCS.NS" to "TCS",
        "INFY.NS" to "Infosys",
        "HDFCBANK.NS" to "HDFC Bank",
        "ICICIBANK.NS" to "ICICI Bank",
        "SBIN.NS" to "SBI",
        "BHARTIARTL.NS" to "Airtel",
        "ITC.NS" to "ITC"
    )

    // Rupee formatter
    private val rupeeFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trade, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user email from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupPopularStocks()
    }

    override fun onResume() {
        super.onResume()
        loadPortfolio()
    }

    private fun initializeViews(view: View) {
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        tvInvestedValue = view.findViewById(R.id.tvInvestedValue)
        tvHoldingsCount = view.findViewById(R.id.tvHoldingsCount)
        rvHoldings = view.findViewById(R.id.rvHoldings)
        emptyHoldingsLayout = view.findViewById(R.id.emptyHoldingsLayout)
        popularStocksLayout = view.findViewById(R.id.popularStocksLayout)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        btnBuyAsset = view.findViewById(R.id.btnBuyAsset)
        btnResetPortfolio = view.findViewById(R.id.btnResetPortfolio)
    }

    private fun setupRecyclerView() {
        holdingsAdapter = HoldingsAdapter(emptyList(), rupeeFormat)
        rvHoldings.layoutManager = LinearLayoutManager(context)
        rvHoldings.adapter = holdingsAdapter
        rvHoldings.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {
        btnBuyAsset.setOnClickListener {
            openBuyActivity(null)
        }

        btnResetPortfolio.setOnClickListener {
            showResetConfirmation()
        }
    }

    private fun setupPopularStocks() {
        popularStocksLayout.removeAllViews()

        popularStocks.forEach { (symbol, name) ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = false
                setChipBackgroundColorResource(R.color.white)
                chipStrokeWidth = 2f
                setChipStrokeColorResource(R.color.finwise_green)
                setTextColor(resources.getColor(R.color.finwise_green, null))
                setOnClickListener {
                    openBuyActivity(symbol)
                }
            }
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 12
            }
            chip.layoutParams = params
            
            popularStocksLayout.addView(chip)
        }
    }

    private fun loadPortfolio() {
        val email = userEmail ?: return

        loadingOverlay.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolio = RetrofitClient.instance.getPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE

                    // Update summary values
                    tvTotalValue.text = rupeeFormat.format(portfolio.total_portfolio_value)
                    tvCashBalance.text = rupeeFormat.format(portfolio.virtual_cash)
                    tvInvestedValue.text = rupeeFormat.format(portfolio.total_holdings_value)

                    // Update holdings list
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Failed to load portfolio: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun showResetConfirmation() {
        AlertDialog.Builder(requireContext(), R.style.FinWiseAlertDialogTheme)
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
                val response = RetrofitClient.instance.resetPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    loadPortfolio() // Refresh the portfolio
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(context, "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
