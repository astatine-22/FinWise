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

// --- Data Models ---

// 1. Auth Models
data class SignupRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleLoginRequest(val token: String)

data class UserProfile(val name: String, val xp: Int)
data class AuthResponse(val message: String, val user_id: Int?, val user: UserProfile?)

// 2. Expense Models
// UPDATED: Added optional 'date' field for sending selected date to backend
data class ExpenseCreateRequest(
    val title: String,
    val amount: Float,
    val category: String,
    val email: String,
    val date: String? = null // Optional ISO 8601 date string
)

data class SimpleResponse(val message: String)

data class ExpenseResponse(
    val id: Int,
    val title: String,
    val amount: Float,
    val category: String,
    val date: String // Formatted date string from backend
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

    // Get Expenses with Date Range Filter
    @GET("api/expenses/{email}")
    suspend fun getExpenses(
        @Path("email") email: String,
        @Query("range") range: String
    ): List<ExpenseResponse>

    // Budget Routes with Date Range Filter
    @GET("api/budget/summary/{email}")
    suspend fun getBudgetSummary(
        @Path("email") email: String,
        @Query("range") range: String
    ): BudgetSummaryResponse

    // Pie Chart Data Route with Date Range Filter
    @GET("api/budget/categories/{email}")
    suspend fun getCategorySummary(
        @Path("email") email: String,
        @Query("range") range: String
    ): List<CategorySummaryResponse>


    // To delete an expense, we need its unique ID
    // We use the HTTP method DELETE
    @DELETE("api/expenses/{id}") // Added api/ prefix to match other routes
    suspend fun deleteExpense(@Path("id") expenseId: Int): SimpleResponse // Changed DeleteResponse to SimpleResponse



    // To update an expense, we need its ID and the new data content
    // We use the HTTP method PUT (or sometimes PATCH) to update
    @PUT("api/expenses/{id}") // Added api/ prefix to match other routes
    suspend fun updateExpense(@Path("id") expenseId: Int, @Body request: ExpenseCreateRequest): ExpenseResponse


}


// --- Retrofit Client (Singleton) ---
object RetrofitClient {
    // IMPORTANT: Use your computer's real local IP address here for physical devices.
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