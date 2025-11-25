package com.example.finwise.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// --- Data Models ---

// 1. Auth Models
data class SignupRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleLoginRequest(val token: String)

data class UserProfile(val name: String, val xp: Int)
data class AuthResponse(val message: String, val user_id: Int?, val user: UserProfile?)

// 2. Expense Models
data class ExpenseCreateRequest(val title: String, val amount: Float, val category: String, val email: String)
data class SimpleResponse(val message: String)
data class ExpenseResponse(
    val id: Int,
    val title: String,
    val amount: Float,
    val category: String,
    val date: String
)

// 3. Budget Models
data class BudgetSummaryResponse(
    val total_spent: Float,
    val limit: Float,
    val remaining: Float
)

// 4. Category Summary Model for Pie Chart
data class CategorySummaryResponse(
    val category: String,
    val total_amount: Float
)


// --- API Interface ---
interface ApiService {

    // Auth Routes
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): AuthResponse

    @GET("api/user/{email}")
    suspend fun getUserDetails(@Path("email") email: String): UserProfile

    // Expense Routes
    @POST("api/expenses")
    suspend fun addExpense(@Body request: ExpenseCreateRequest): SimpleResponse

    @GET("api/expenses/{email}")
    suspend fun getExpenses(@Path("email") email: String): List<ExpenseResponse>

    // Budget Routes
    @GET("api/budget/summary/{email}")
    suspend fun getBudgetSummary(@Path("email") email: String): BudgetSummaryResponse

    // Pie Chart Data Route
    @GET("api/budget/categories/{email}")
    suspend fun getCategorySummary(@Path("email") email: String): List<CategorySummaryResponse>
}


// --- Retrofit Client (Singleton) ---
object RetrofitClient {
    // IMPORTANT: Updated with your physical phone's local network IP address.
    // Keep http:// at the start and :8000/ at the end.
    private const val BASE_URL = "http://172.18.0.149:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}