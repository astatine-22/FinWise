package com.example.finwise.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ============================================================================
// AUTH MODELS
// ============================================================================

data class SignupRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleLoginRequest(val token: String)

data class UserProfile(val name: String, val xp: Int)
data class AuthResponse(val message: String, val user_id: Int?, val user: UserProfile?)

// ============================================================================
// EXPENSE & BUDGET MODELS
// ============================================================================

data class ExpenseCreateRequest(
    val title: String,
    val amount: Float,
    val category: String,
    val email: String,
    val date: String? = null
)

data class SimpleResponse(val message: String)

data class ExpenseResponse(
    val id: Int,
    val title: String,
    val amount: Float,
    val category: String,
    val date: String
)

data class BudgetSummaryResponse(
    val total_spent: Float,
    val limit: Float,
    val remaining: Float
)

data class CategorySummaryResponse(
    val category: String,
    val total_amount: Float
)

// ============================================================================
// LEARN MODULE MODELS
// ============================================================================

data class LearnVideoResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val thumbnail_url: String?,
    val youtube_video_id: String,
    val category: String,
    val duration_minutes: Int?,
    val is_featured: Boolean
)

// ============================================================================
// PAPER TRADING MODELS (All values in Indian Rupees ₹)
// ============================================================================

data class HoldingResponse(
    val asset_symbol: String,
    val quantity: Float,
    val average_buy_price: Float,
    val current_price: Float?,
    val current_value: Float?,
    val profit_loss: Float?,
    val profit_loss_percent: Float?
)

data class PortfolioSummaryResponse(
    val portfolio_id: Int,
    val virtual_cash: Float,
    val total_holdings_value: Float,
    val total_portfolio_value: Float,
    val holdings: List<HoldingResponse>
)

data class TradeRequest(
    val asset_symbol: String,
    val quantity: Float
)

data class TradeExecutionResponse(
    val message: String,
    val asset_symbol: String,
    val quantity: Float,
    val executed_price: Float,
    val total_cost: Float,
    val remaining_cash: Float,
    val new_holding_quantity: Float,
    val new_average_price: Float
)

data class AssetPriceResponse(
    val symbol: String,
    val current_price: Float?,
    val currency: String,
    val source: String
)

data class StockSearchResult(
    val name: String,
    val symbol: String,
    val exchange: String?
)

// Price History Models (for charts)
data class PricePoint(
    val timestamp: Long,
    val price: Float
)

data class PriceHistoryResponse(
    val symbol: String,
    val period: String,
    val data: List<PricePoint>,
    val price_change: Float,
    val price_change_percent: Float,
    val current_price: Float,
    val previous_close: Float
)

// ============================================================================
// API INTERFACE
// ============================================================================

interface ApiService {

    // --- Auth Routes ---
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse

    @GET("api/user/{email}")
    suspend fun getUserDetails(@Path("email") email: String): UserProfile

    // --- Expense Routes ---
    @POST("api/expenses")
    suspend fun addExpense(@Body request: ExpenseCreateRequest): SimpleResponse

    @GET("api/expenses/{email}")
    suspend fun getExpenses(
        @Path("email") email: String,
        @Query("range") range: String
    ): List<ExpenseResponse>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") expenseId: Int): SimpleResponse

    @PUT("api/expenses/{id}")
    suspend fun updateExpense(@Path("id") expenseId: Int, @Body request: ExpenseCreateRequest): ExpenseResponse

    // --- Budget Routes ---
    @GET("api/budget/summary/{email}")
    suspend fun getBudgetSummary(
        @Path("email") email: String,
        @Query("range") range: String
    ): BudgetSummaryResponse

    @GET("api/budget/categories/{email}")
    suspend fun getCategorySummary(
        @Path("email") email: String,
        @Query("range") range: String
    ): List<CategorySummaryResponse>

    // --- Learn Module Routes ---
    @GET("api/learn/videos")
    suspend fun getLearnVideos(
        @Query("category") category: String? = null
    ): List<LearnVideoResponse>

    @GET("api/learn/categories")
    suspend fun getLearnCategories(): List<String>

    @POST("api/learn/seed")
    suspend fun seedLearnVideos(): SimpleResponse

    // --- Paper Trading Routes (All values in ₹) ---
    @GET("api/trade/portfolio")
    suspend fun getPortfolio(
        @Query("email") email: String
    ): PortfolioSummaryResponse

    @GET("api/trade/holdings")
    suspend fun getHoldings(
        @Query("email") email: String
    ): List<HoldingResponse>

    @POST("api/trade/buy")
    suspend fun executeBuyOrder(
        @Body request: TradeRequest,
        @Query("email") email: String
    ): TradeExecutionResponse

    @GET("api/trade/price/{symbol}")
    suspend fun getAssetPrice(
        @Path("symbol") symbol: String
    ): AssetPriceResponse

    @POST("api/trade/reset")
    suspend fun resetPortfolio(
        @Query("email") email: String
    ): SimpleResponse

    @GET("api/trade/search")
    suspend fun searchStocks(
        @Query("query") query: String
    ): List<StockSearchResult>

    @GET("api/trade/history/{symbol}")
    suspend fun getPriceHistory(
        @Path("symbol") symbol: String,
        @Query("period") period: String = "1d"
    ): PriceHistoryResponse
}

// ============================================================================
// RETROFIT CLIENT (Singleton)
// ============================================================================

object RetrofitClient {
    // IMPORTANT: Use your computer's real local IP address here for physical devices.
    private const val BASE_URL = "http://172.18.0.149:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}