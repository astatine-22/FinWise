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
import com.example.finwise.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class PortfolioFragment : Fragment() {

    private lateinit var tvTotalValue: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvInvestedValue: TextView
    private lateinit var tvHoldingsCount: TextView
    private lateinit var rvHoldings: RecyclerView
    private lateinit var emptyHoldingsLayout: LinearLayout
    private lateinit var btnBuyAsset: MaterialButton
    private lateinit var btnResetPortfolio: MaterialButton

    private lateinit var holdingsAdapter: HoldingsAdapter
    private var userEmail: String? = null
    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Loading overlay - we'll handle this within the fragment
    private var loadingOverlay: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        // Try to get loading overlay from parent activity
        loadingOverlay = activity?.findViewById(R.id.loadingOverlay)

        initializeViews(view)
        setupAdapter()
        setupListeners()

        // Load portfolio data
        loadPortfolio()
    }

    override fun onResume() {
        super.onResume()
        // Reload portfolio when fragment becomes visible again
        loadPortfolio()
    }

    private fun initializeViews(view: View) {
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        tvInvestedValue = view.findViewById(R.id.tvInvestedValue)
        tvHoldingsCount = view.findViewById(R.id.tvHoldingsCount)
        rvHoldings = view.findViewById(R.id.rvHoldings)
        emptyHoldingsLayout = view.findViewById(R.id.emptyHoldingsLayout)
        btnBuyAsset = view.findViewById(R.id.btnBuyAsset)
        btnResetPortfolio = view.findViewById(R.id.btnResetPortfolio)
    }

    private fun setupAdapter() {
        holdingsAdapter = HoldingsAdapter(emptyList(), rupeeFormat) { holding ->
            // When item clicked, open BuyAssetActivity with holding details
            val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
                putExtra("USER_EMAIL", userEmail)
                putExtra("ASSET_SYMBOL", holding.asset_symbol)
                putExtra("ASSET_NAME", holding.asset_symbol.replace(".NS", "").replace(".BO", ""))
                putExtra("CURRENT_PRICE", holding.current_value?.div(holding.quantity) ?: holding.average_buy_price)
                putExtra("OWNED_QUANTITY", holding.quantity)
            }
            startActivity(intent)
        }
        rvHoldings.layoutManager = LinearLayoutManager(context)
        rvHoldings.adapter = holdingsAdapter
    }

    private fun setupListeners() {
        btnBuyAsset.setOnClickListener { openBuyActivity(null) }
        btnResetPortfolio.setOnClickListener { showResetConfirmation() }
    }

    private fun loadPortfolio() {
        val email = userEmail ?: return
        loadingOverlay?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val portfolio = RetrofitClient.instance.getPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay?.visibility = View.GONE
                    displayPortfolio(portfolio)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay?.visibility = View.GONE
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
        loadingOverlay?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.resetPortfolio(email)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay?.visibility = View.GONE
                    Toast.makeText(context, "Portfolio reset successfully!", Toast.LENGTH_SHORT).show()
                    loadPortfolio()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    loadingOverlay?.visibility = View.GONE
                    Toast.makeText(context, "Failed to reset portfolio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
