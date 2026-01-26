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

data class UserProfile(val name: String, val xp: Int, val profile_picture: String? = null)

// New token-based authentication response
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val user_id: Int? = null,     // Returned from login endpoint
    val name: String? = null,      // User's name from login
    val user: UserProfile? = null  // For backward compatibility with Google login
)

// Legacy response (for backward compatibility)
data class AuthResponse(val message: String, val user_id: Int?, val user: UserProfile?)

data class ProfilePictureUpdate(val email: String, val profile_picture: String)

data class ProfileUpdate(val email: String, val name: String? = null, val profile_picture: String? = null)


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
    val is_featured: Boolean,
    val embed_url: String?  // Full YouTube embed URL: https://www.youtube.com/embed/{video_id}
)

data class LessonCompleteRequest(
    val email: String,
    val video_id: Int
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

// Sell Request/Response
data class SellRequest(
    val email: String,
    val symbol: String,
    val quantity: Float
)

data class SellExecutionResponse(
    val message: String,
    val asset_symbol: String,
    val quantity_sold: Float,
    val executed_price: Float,
    val total_proceeds: Float,
    val remaining_cash: Float,
    val remaining_quantity: Float
)

// Budget Limit Update
data class BudgetLimitUpdate(
    val email: String,
    val budget_limit: Float
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
// MARKET DATA MODELS (INDmoney-style)
// ============================================================================

data class MarketIndex(
    val name: String,
    val symbol: String,
    val value: Float,
    val change: Float,
    val change_percent: Float,
    val is_positive: Boolean
)

data class MarketIndicesResponse(
    val indices: List<MarketIndex>
)

data class StockItem(
    val symbol: String,
    val name: String,
    val sector: String?,
    val price: Float,
    val change: Float,
    val change_percent: Float,
    val is_positive: Boolean,
    val logo_initial: String
)

data class StockListResponse(
    val stocks: List<StockItem>
)

// ============================================================================
// CRYPTO MODELS
// ============================================================================

data class CryptoItem(
    val symbol: String,
    val name: String,
    val short_name: String,
    val price_usd: Float,
    val price_inr: Float,
    val change_24h: Float,
    val change_percent_24h: Float,
    val is_positive: Boolean,
    val logo_initial: String
)

data class CryptoListResponse(
    val cryptos: List<CryptoItem>,
    val usd_to_inr: Float
)

// ============================================================================
// GAMIFICATION MODELS
// ============================================================================

data class GamificationResponse(
    val xp: Int,
    val level: Int,
    val level_title: String,
    val progress_to_next: Float,    // 0.0 to 1.0
    val xp_for_next_level: Int,
    val current_streak: Int,
    val earned_achievements: List<String>  // List of achievement keys
)

data class AchievementDef(
    val key: String,
    val name: String,
    val description: String,
    val xp_reward: Int,
    val icon_name: String
)

// ============================================================================
// LEADERBOARD MODELS (Hall of Fame)
// ============================================================================

data class LeaderboardEntry(
    val rank: Int,
    val display_name: String,  // Privacy-safe: "John D."
    val xp: Int,
    val profile_picture: String? = null  // Base64 encoded image
)

data class LeaderboardResponse(
    val top_users: List<LeaderboardEntry>,
    val user_rank: Int,
    val user_xp: Int,
    val user_display_name: String,
    val user_profile_picture: String? = null
)

// ============================================================================
// API INTERFACE
// ============================================================================

interface ApiService {

    // --- Auth Routes (now returning JWT tokens) ---
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): SimpleResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): TokenResponse

    @GET("api/user/{email}")
    suspend fun getUserDetails(@Path("email") email: String): UserProfile

    @PUT("api/user/profile-picture")
    suspend fun updateProfilePicture(@Body request: ProfilePictureUpdate): SimpleResponse

    @PUT("api/user/profile")
    suspend fun updateProfile(@Body request: ProfileUpdate): SimpleResponse

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

    @POST("api/learn/complete")
    suspend fun completeLesson(@Body request: LessonCompleteRequest): SimpleResponse

    // --- Quiz Routes (for Learn Module) ---
    @GET("api/learn/quiz/{videoId}")
    suspend fun getQuiz(@Path("videoId") videoId: Int): Quiz

    @POST("api/learn/quiz/submit")
    suspend fun submitQuiz(@Body submission: QuizSubmission): QuizResult

    @POST("api/learn/quiz/check-answer")
    suspend fun checkAnswer(@Body request: CheckAnswerRequest): CheckAnswerResponse

    @POST("api/learn/quiz/claim-bonus")
    suspend fun claimBonus(@Body request: ClaimBonusRequest): BonusResponse

    @POST("api/learn/seed-v2")
    suspend fun seedLearnV2(): SimpleResponse

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

    @POST("api/trade/sell")
    suspend fun executeSellOrder(
        @Body request: SellRequest
    ): SellExecutionResponse

    @PUT("api/user/budget-limit")
    suspend fun updateBudgetLimit(
        @Body request: BudgetLimitUpdate
    ): SimpleResponse

    // --- Gamification Routes ---
    @GET("api/user/gamification/{email}")
    suspend fun getUserGamification(
        @Path("email") email: String
    ): GamificationResponse

    @GET("api/achievements/all")
    suspend fun getAllAchievements(): List<AchievementDef>

    @POST("api/achievements/seed")
    suspend fun seedAchievements(): SimpleResponse

    // --- Leaderboard (Hall of Fame) ---
    @GET("api/leaderboard")
    suspend fun getLeaderboard(
        @Query("email") email: String
    ): LeaderboardResponse

    // --- Market Data (INDmoney-style) ---
    @GET("api/market/indices")
    suspend fun getMarketIndices(): MarketIndicesResponse

    @GET("api/market/stocks")
    suspend fun getAllStocks(): StockListResponse

    @GET("api/market/top-gainers")
    suspend fun getTopGainers(): StockListResponse

    @GET("api/market/top-losers")
    suspend fun getTopLosers(): StockListResponse

    // --- Crypto Routes ---
    @GET("api/crypto/list")
    suspend fun getCryptoList(): CryptoListResponse

    @GET("api/crypto/top-gainers")
    suspend fun getCryptoGainers(): CryptoListResponse

    // --- Separate Stock Markets ---
    @GET("api/market/stocks/us")
    suspend fun getUsStocks(): StockListResponse

    @GET("api/market/stocks/in")
    suspend fun getIndianStocks(): StockListResponse
}

// ============================================================================
// RETROFIT CLIENT (Singleton with JWT Authentication)
// ============================================================================

object RetrofitClient {
    // Production URL (Render cloud deployment)
    private const val BASE_URL = "https://finwise-api-z29q.onrender.com/"
    
    // Development URL (Android Emulator -> Localhost)
    // private const val BASE_URL = "http://10.0.2.2:8000/"

    @Volatile
    private var apiService: ApiService? = null
    
    @Volatile
    private var sessionManager: SessionManager? = null

    /**
     * Initialize RetrofitClient with application context.
     * This MUST be called once from Application.onCreate() or before first API call.
     */
    fun initialize(context: android.content.Context) {
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance(context.applicationContext)
        }
    }

    /**
     * Get the API service instance.
     * If not initialized, creates a basic client without auth interceptor.
     */
    val instance: ApiService
        get() {
            return apiService ?: synchronized(this) {
                apiService ?: createApiService().also { apiService = it }
            }
        }

    private fun createApiService(): ApiService {
        // Extended timeouts for Render cloud cold starts (can take 60+ seconds)
        val okHttpClientBuilder = okhttp3.OkHttpClient.Builder()
            .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)

        // Add AuthInterceptor if SessionManager is initialized
        sessionManager?.let { sm ->
            okHttpClientBuilder.addInterceptor(AuthInterceptor(sm))
        }

        val okHttpClient = okHttpClientBuilder.build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /**
     * Force recreate the API service instance.
     * Call this after login to ensure the new token is picked up by the interceptor.
     */
    fun refreshClient() {
        synchronized(this) {
            apiService = null
        }
    }
}