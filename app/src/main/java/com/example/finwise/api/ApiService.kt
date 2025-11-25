package com.example.finwise.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// --- DATA MODELS ---

// 1. Auth Models
data class SignupRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val message: String,
    val user_id: Int?,
    val user: UserProfile?
)

data class UserProfile(val name: String, val xp: Int)

// 2. Budget View Models
data class BudgetSummaryResponse(
    val total_spent: Double,
    val limit: Double,
    val remaining: Double
)

data class ExpenseItem(
    val id: Int,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String
)

// 3. Add Expense Models (NEW)
data class ExpenseCreateRequest(
    val title: String,
    val amount: Double,
    val category: String,
    val email: String
)

data class SimpleResponse(val message: String)


// --- API INTERFACE ---

interface ApiService {

    // Authentication
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // User Info
    @GET("api/user/{email}")
    suspend fun getUserDetails(@Path("email") email: String): UserProfile

    // Budget - Get Data
    @GET("api/expenses/{email}")
    suspend fun getExpenses(@Path("email") email: String): List<ExpenseItem>

    @GET("api/budget/summary/{email}")
    suspend fun getBudgetSummary(@Path("email") email: String): BudgetSummaryResponse

    // Budget - Add Data (NEW)
    @POST("api/expenses")
    suspend fun addExpense(@Body request: ExpenseCreateRequest): SimpleResponse
}