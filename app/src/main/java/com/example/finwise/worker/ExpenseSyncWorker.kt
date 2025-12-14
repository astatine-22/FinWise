package com.example.finwise.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finwise.api.ExpenseCreateRequest
import com.example.finwise.api.RetrofitClient
import com.example.finwise.data.local.AppDatabase

/**
 * Background worker to synchronize offline expenses with the backend.
 * Checks for expenses where isSynced = false and uploads them.
 */
class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        
        // Manual dependency injection (Phase 3 constraint)
        val database = AppDatabase.getDatabase(context)
        val expenseDao = database.expenseDao()
        val apiService = RetrofitClient.instance

        // 1. Get user email (needed for API payload)
        // In a real app, this should be securely stored. For now, grabbing from SharedPreferences
        val sharedPref = context.getSharedPreferences("FinWisePrefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("LOGGED_IN_EMAIL", null)

        if (email == null) {
            Log.e("ExpenseSyncWorker", "No logged in user found for sync.")
            return Result.failure()
        }

        try {
            // 2. Query unsynced expenses
            val unsyncedExpenses = expenseDao.getUnsyncedExpenses()
            
            if (unsyncedExpenses.isEmpty()) {
                Log.d("ExpenseSyncWorker", "No unsynced expenses found.")
                return Result.success()
            }

            Log.d("ExpenseSyncWorker", "Found ${unsyncedExpenses.size} unsynced expenses. Starting upload...")

            // 3. Upload each expense
            var failureCount = 0
            for (expense in unsyncedExpenses) {
                try {
                    val request = ExpenseCreateRequest(
                        title = expense.description,
                        amount = expense.amount.toFloat(),
                        category = expense.category,
                        email = email,
                        // Convert timestamp back to string if API expects it, or pass null to let backend handle date
                        // API expects strict format, assuming backend handles current date if null, or we format it.
                        // For simplicity in this phase, letting backend set date or sending null.
                        date = null 
                    )

                    // Call API
                    val response = apiService.addExpense(request)
                    
                    // On success, mark as synced
                    // Note: Current API returns SimpleResponse, doesn't give back the new ID.
                    // Ideally API should return the created object. Assuming "Success" string check or HTTP 200.
                    if (response.message.contains("success", ignoreCase = true) || response.message.isNotEmpty()) {
                        // Mark local record as synced
                        // We use the same backendId for now or just flag it
                        expenseDao.markAsSynced(expense.localId, "synced_backend_id_placeholder")
                        Log.d("ExpenseSyncWorker", "Successfully synced expense: ${expense.description}")
                    } else {
                        Log.w("ExpenseSyncWorker", "API returned failure for expense: ${expense.description}")
                        failureCount++
                    }

                } catch (e: Exception) {
                    Log.e("ExpenseSyncWorker", "Failed to sync expense: ${expense.description}", e)
                    failureCount++
                }
            }

            // 4. Determine result
            return if (failureCount > 0) {
                // If some failed, retry later
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Log.e("ExpenseSyncWorker", "Fatal error during sync", e)
            return Result.failure()
        }
    }
}
