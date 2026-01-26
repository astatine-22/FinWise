package com.example.finwise.data.repository

import android.util.Log
import com.example.finwise.api.ApiService
import com.example.finwise.data.local.dao.ExpenseDao
import com.example.finwise.data.local.entity.LocalExpense
import com.example.finwise.data.toLocalExpenses
import kotlinx.coroutines.flow.Flow

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.finwise.worker.ExpenseSyncWorker

/**
 * Repository for managing Expense/Budget data.
 * Mediate between local database (ExpenseDao) and remote API (ApiService).
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val apiService: ApiService,
    private val workManager: WorkManager
) {

    /**
     * Source of truth: Flow from the local database (all expenses).
     * The UI should observe this.
     */
    val allExpenses: Flow<List<LocalExpense>> = expenseDao.getAllExpenses()

    /**
     * Add a new expense.
     * OFFLINE-FIRST APPROACH:
     * 1. Write to Local DB immediately (isSynced = false).
     * 2. Enqueue Background Sync job.
     */
    suspend fun addNewExpense(amount: Double, category: String, description: String) {
        // 1. Create Local Entity (Offline)
        val newExpense = LocalExpense(
            amount = amount,
            category = category,
            description = description,
            dateTimestamp = System.currentTimeMillis(),
            isSynced = false // Not synced yet
        )

        // 2. Insert into DB (Updates UI immediately via Flow)
        expenseDao.insertSingleExpense(newExpense)

        // 3. Schedule Background Sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequest.Builder(ExpenseSyncWorker::class.java)
            .setConstraints(constraints)
            .build()

        // Enqueue unique work to avoid duplicate jobs stacking up blindly
        workManager.enqueueUniqueWork(
            "ExpenseSyncWork",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }

    /**
     * Refresh expenses from the API and replace the local cache.
     * Use "1M" (1 month) or "all" range logic as needed. Defaulting to fetching recent ones.
     */
    suspend fun refreshExpenses(email: String) {
        try {
            // 1. Fetch from API (fetching 'all' or a large range to populate cache)
            // TODO: Ideally support pagination or smarter sync. For now, fetching last 30 days or similar.
            val expenseResponses = apiService.getExpenses(email, "1Y") // Fetching 1 Year for robust cache

            // 2. Map to Local Entities
            val localExpenses = expenseResponses.toLocalExpenses()

            // 3. Clear existing SYNCED data to avoid stale duplicates
            // We keep unsynced data (isSynced=false) to push later
            expenseDao.deleteSyncedExpenses()

            // 4. Insert new data from server
            if (localExpenses.isNotEmpty()) {
                expenseDao.insertExpenses(localExpenses)
            }

            Log.d("ExpenseRepository", "Expenses refreshed successfully: ${localExpenses.size} items")

        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Failed to refresh expenses", e)
            throw e // Re-throw to let UI handle error state
        }
    }
    
    /**
     * Clear expense data on logout.
     */
    suspend fun clearUserData() {
        expenseDao.deleteAllExpenses()
    }
}
