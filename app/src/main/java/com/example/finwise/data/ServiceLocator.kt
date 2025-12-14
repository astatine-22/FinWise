package com.example.finwise.data

import android.content.Context
import androidx.work.WorkManager
import com.example.finwise.api.RetrofitClient
import com.example.finwise.data.local.AppDatabase
import com.example.finwise.data.repository.ExpenseRepository
import com.example.finwise.data.repository.UserRepository

/**
 * Simple Service Locator for manual dependency injection.
 * In a production app, use Hilt or Koin instead.
 */
object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var expenseRepository: ExpenseRepository? = null

    @Volatile
    private var userRepository: UserRepository? = null

    /**
     * Get or create ExpenseRepository instance.
     */
    fun getExpenseRepository(context: Context): ExpenseRepository {
        return expenseRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = ExpenseRepository(
                expenseDao = db.expenseDao(),
                apiService = RetrofitClient.instance,
                workManager = WorkManager.getInstance(context.applicationContext)
            )
            expenseRepository = repo
            repo
        }
    }

    /**
     * Get or create UserRepository instance.
     */
    fun getUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = UserRepository(
                userDao = db.userDao(),
                apiService = RetrofitClient.instance
            )
            userRepository = repo
            repo
        }
    }

    private fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            database = db
            db
        }
    }

    /**
     * Clear all cached instances (call on logout).
     */
    fun resetAll() {
        expenseRepository = null
        userRepository = null
        // Don't clear database reference, just the repositories
    }
}
