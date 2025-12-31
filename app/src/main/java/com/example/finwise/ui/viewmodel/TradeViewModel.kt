package com.example.finwise.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finwise.api.CryptoItem
import com.example.finwise.api.MarketIndex
import com.example.finwise.api.PortfolioSummaryResponse
import com.example.finwise.api.StockItem
import com.example.finwise.data.repository.TradeRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Single-use event wrapper for error messages.
 * Prevents re-showing errors on configuration change.
 */
class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}

/**
 * Filter types for stock list.
 */
enum class StockFilterType {
    EXPLORE,
    GAINERS,
    LOSERS
}

/**
 * ViewModel for the Trade feature.
 * Manages state for stocks, crypto, portfolio, and handles client-side filtering.
 */
class TradeViewModel(
    private val repository: TradeRepository,
    private val userEmail: String
) : ViewModel() {

    companion object {
        private const val TAG = "TradeViewModel"
    }

    // =============================
    // STATE: Market Indices
    // =============================
    private val _indices = MutableLiveData<List<MarketIndex>>()
    val indices: LiveData<List<MarketIndex>> = _indices

    // =============================
    // STATE: Stocks
    // =============================
    private var originalStockList: List<StockItem> = emptyList()
    private var currentFilter = StockFilterType.EXPLORE

    private val _stocks = MutableLiveData<List<StockItem>>()
    val stocks: LiveData<List<StockItem>> = _stocks

    private val _isStocksLoading = MutableLiveData<Boolean>()
    val isStocksLoading: LiveData<Boolean> = _isStocksLoading

    // =============================
    // STATE: Crypto
    // =============================
    private val _cryptos = MutableLiveData<List<CryptoItem>>()
    val cryptos: LiveData<List<CryptoItem>> = _cryptos

    private val _isCryptoLoading = MutableLiveData<Boolean>()
    val isCryptoLoading: LiveData<Boolean> = _isCryptoLoading

    // =============================
    // STATE: Portfolio
    // =============================
    private val _portfolio = MutableLiveData<PortfolioSummaryResponse?>()
    val portfolio: LiveData<PortfolioSummaryResponse?> = _portfolio

    private val _walletBalance = MutableLiveData<Float>()
    val walletBalance: LiveData<Float> = _walletBalance

    private val _isPortfolioLoading = MutableLiveData<Boolean>()
    val isPortfolioLoading: LiveData<Boolean> = _isPortfolioLoading

    // =============================
    // STATE: Errors & Events
    // =============================
    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = _error

    private val _unauthorizedEvent = MutableLiveData<Event<Unit>>()
    val unauthorizedEvent: LiveData<Event<Unit>> = _unauthorizedEvent

    // =============================
    // PUBLIC FUNCTIONS
    // =============================

    /**
     * Load initial data for the Stocks tab.
     * Fetches market indices and all stocks.
     */
    fun loadInitialData() {
        loadMarketIndices()
        loadStocks()
    }

    /**
     * Load market indices (NIFTY, SENSEX, etc.)
     */
    fun loadMarketIndices() {
        viewModelScope.launch {
            repository.getMarketIndices()
                .onSuccess { response ->
                    _indices.value = response.indices
                }
                .onFailure { e ->
                    handleError(e, "Failed to load market indices")
                }
        }
    }

    /**
     * Load all stocks.
     * Stores the original list for client-side filtering.
     */
    fun loadStocks() {
        _isStocksLoading.value = true
        
        viewModelScope.launch {
            repository.getStocks()
                .onSuccess { response ->
                    _isStocksLoading.value = false
                    originalStockList = response.stocks
                    
                    if (response.stocks.isEmpty()) {
                        Log.d(TAG, "Stocks response is empty")
                    } else {
                        Log.d(TAG, "Loaded ${response.stocks.size} stocks")
                    }
                    
                    // Apply current filter to the loaded data
                    applyCurrentFilter()
                }
                .onFailure { e ->
                    _isStocksLoading.value = false
                    handleError(e, "Failed to load stocks")
                }
        }
    }

    /**
     * Filter stocks by type.
     * This is a CLIENT-SIDE operation - no API call is made.
     * 
     * @param filterType One of: EXPLORE, GAINERS, LOSERS
     */
    fun filterStocks(filterType: StockFilterType) {
        currentFilter = filterType
        applyCurrentFilter()
    }

    /**
     * Convenience method to filter by string type (for chip click listeners).
     * 
     * @param filterType One of: "EXPLORE", "GAINERS", "LOSERS"
     */
    fun filterStocks(filterType: String) {
        val type = when (filterType.uppercase()) {
            "GAINERS" -> StockFilterType.GAINERS
            "LOSERS" -> StockFilterType.LOSERS
            else -> StockFilterType.EXPLORE
        }
        filterStocks(type)
    }

    /**
     * Load cryptocurrency list.
     */
    fun loadCryptos() {
        _isCryptoLoading.value = true
        
        viewModelScope.launch {
            repository.getCrypto()
                .onSuccess { response ->
                    _isCryptoLoading.value = false
                    _cryptos.value = response.cryptos
                    
                    if (response.cryptos.isEmpty()) {
                        Log.d(TAG, "Crypto response is empty")
                    } else {
                        Log.d(TAG, "Loaded ${response.cryptos.size} cryptocurrencies")
                    }
                }
                .onFailure { e ->
                    _isCryptoLoading.value = false
                    handleError(e, "Failed to load cryptocurrencies")
                }
        }
    }

    /**
     * Load user's portfolio.
     */
    fun loadPortfolio() {
        _isPortfolioLoading.value = true
        
        viewModelScope.launch {
            repository.getPortfolio(userEmail)
                .onSuccess { response ->
                    _isPortfolioLoading.value = false
                    _portfolio.value = response
                    _walletBalance.value = response.virtual_cash
                }
                .onFailure { e ->
                    _isPortfolioLoading.value = false
                    handleError(e, "Failed to load portfolio")
                }
        }
    }

    /**
     * Load wallet balance only (for Stocks tab hero card).
     */
    fun loadWalletBalance() {
        viewModelScope.launch {
            repository.getPortfolio(userEmail)
                .onSuccess { response ->
                    _walletBalance.value = response.virtual_cash
                }
                .onFailure { e ->
                    handleError(e, "Failed to load wallet balance")
                }
        }
    }

    /**
     * Reset user's portfolio to initial state.
     */
    fun resetPortfolio() {
        _isPortfolioLoading.value = true
        
        viewModelScope.launch {
            repository.resetPortfolio(userEmail)
                .onSuccess { response ->
                    Log.d(TAG, "Portfolio reset: ${response.message}")
                    // Reload portfolio after reset
                    loadPortfolio()
                }
                .onFailure { e ->
                    _isPortfolioLoading.value = false
                    handleError(e, "Failed to reset portfolio")
                }
        }
    }

    // =============================
    // PRIVATE FUNCTIONS
    // =============================

    /**
     * Apply the current filter to the original stock list.
     * This is a pure client-side operation.
     */
    private fun applyCurrentFilter() {
        if (originalStockList.isEmpty()) {
            Log.d(TAG, "Stock list is empty, cannot apply filter")
            _stocks.value = emptyList()
            return
        }

        val filteredList = when (currentFilter) {
            StockFilterType.GAINERS -> {
                // Filter positive changes, sort by change_percent descending (highest first)
                originalStockList
                    .filter { it.is_positive && it.change_percent > 0 }
                    .sortedByDescending { it.change_percent }
            }
            StockFilterType.LOSERS -> {
                // Filter negative changes, sort by change_percent ascending (most negative first)
                originalStockList
                    .filter { !it.is_positive || it.change_percent < 0 }
                    .sortedBy { it.change_percent }
            }
            StockFilterType.EXPLORE -> {
                // Return original list unchanged
                originalStockList
            }
        }

        Log.d(TAG, "Applied filter '${currentFilter.name}': ${filteredList.size} stocks")
        _stocks.value = filteredList
    }

    /**
     * Handle errors, checking for 401 Unauthorized.
     */
    private fun handleError(e: Throwable, defaultMessage: String) {
        Log.e(TAG, "$defaultMessage: ${e.message}")
        
        if (e is HttpException && e.code() == 401) {
            _unauthorizedEvent.value = Event(Unit)
        } else {
            val errorMessage = when (e) {
                is HttpException -> "Server error: ${e.code()}"
                else -> e.message ?: defaultMessage
            }
            _error.value = Event(errorMessage)
        }
    }
}

/**
 * Factory for creating TradeViewModel with dependencies.
 */
class TradeViewModelFactory(
    private val repository: TradeRepository,
    private val userEmail: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TradeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TradeViewModel(repository, userEmail) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
