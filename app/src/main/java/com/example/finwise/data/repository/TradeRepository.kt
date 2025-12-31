package com.example.finwise.data.repository

import android.util.Log
import com.example.finwise.api.ApiService
import com.example.finwise.api.CryptoListResponse
import com.example.finwise.api.MarketIndicesResponse
import com.example.finwise.api.PortfolioSummaryResponse
import com.example.finwise.api.SimpleResponse
import com.example.finwise.api.StockListResponse

/**
 * Repository for Trade feature data operations.
 * Abstracts API calls from ViewModel for cleaner architecture.
 */
class TradeRepository(
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "TradeRepository"
    }

    /**
     * Fetch market indices (NIFTY, SENSEX, etc.)
     */
    suspend fun getMarketIndices(): Result<MarketIndicesResponse> {
        return try {
            val response = apiService.getMarketIndices()
            Log.d(TAG, "Fetched ${response.indices.size} market indices")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch market indices: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch all stocks.
     * Note: We only fetch ALL stocks, not gainers/losers separately.
     * Filtering is done client-side in the ViewModel.
     */
    suspend fun getStocks(): Result<StockListResponse> {
        return try {
            val response = apiService.getAllStocks()
            Log.d(TAG, "Fetched ${response.stocks.size} stocks")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stocks: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch cryptocurrency list.
     */
    suspend fun getCrypto(): Result<CryptoListResponse> {
        return try {
            val response = apiService.getCryptoList()
            Log.d(TAG, "Fetched ${response.cryptos.size} cryptocurrencies")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch crypto: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetch user's portfolio.
     */
    suspend fun getPortfolio(email: String): Result<PortfolioSummaryResponse> {
        return try {
            val response = apiService.getPortfolio(email)
            Log.d(TAG, "Fetched portfolio for $email: ${response.holdings.size} holdings")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch portfolio: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Reset user's portfolio to initial state.
     */
    suspend fun resetPortfolio(email: String): Result<SimpleResponse> {
        return try {
            val response = apiService.resetPortfolio(email)
            Log.d(TAG, "Portfolio reset: ${response.message}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset portfolio: ${e.message}")
            Result.failure(e)
        }
    }
}
