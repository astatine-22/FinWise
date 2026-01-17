package com.example.finwise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.api.RetrofitClient
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CryptoMarketFragment : Fragment() {

    private lateinit var rvCrypto: RecyclerView
    private lateinit var shimmerCrypto: ShimmerFrameLayout

    private lateinit var cryptoCardAdapter: CryptoCardAdapter
    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crypto_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        userEmail = sharedPref.getString("LOGGED_IN_EMAIL", null)

        initializeViews(view)
        setupAdapter()

        // Load cryptocurrencies
        loadCryptos()
    }

    private fun initializeViews(view: View) {
        rvCrypto = view.findViewById(R.id.rvCrypto)
        shimmerCrypto = view.findViewById(R.id.shimmerCrypto)
    }

    private fun setupAdapter() {
        cryptoCardAdapter = CryptoCardAdapter(emptyList()) { crypto ->
            openBuyActivity(crypto.symbol)
        }
        rvCrypto.layoutManager = LinearLayoutManager(context)
        rvCrypto.adapter = cryptoCardAdapter
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

    private fun openBuyActivity(prefilledSymbol: String?) {
        val intent = Intent(requireContext(), BuyAssetActivity::class.java).apply {
            putExtra("USER_EMAIL", userEmail)
            prefilledSymbol?.let { putExtra("PREFILLED_SYMBOL", it) }
        }
        startActivity(intent)
    }
}
