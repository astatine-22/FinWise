package com.example.finwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finwise.data.local.entity.LocalExpense
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LocalExpense entity.
 * Provides database operations for expense/budget data.
 */
@Dao
interface ExpenseDao {

    /**
     * Insert or replace a list of expenses.
     * Used for caching expenses fetched from the backend.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<LocalExpense>)

    /**
     * Insert a single expense and return its generated localId.
     * Used when user adds a new expense offline.
     */
    @Insert
    suspend fun insertSingleExpense(expense: LocalExpense): Long

    /**
     * Get all expenses ordered by date (newest first) as a Flow.
     * Provides reactive updates when data changes.
     */
    @Query("SELECT * FROM expense_table ORDER BY dateTimestamp DESC")
    fun getAllExpenses(): Flow<List<LocalExpense>>

    /**
     * Get expenses that haven't been synced to the backend yet.
     * Used by sync logic to find "dirty" records to upload.
     */
    @Query("SELECT * FROM expense_table WHERE isSynced = 0")
    suspend fun getUnsyncedExpenses(): List<LocalExpense>

    /**
     * Mark an expense as synced after successful backend upload.
     */
    @Query("UPDATE expense_table SET isSynced = 1, backendId = :backendId WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long, backendId: String)

    /**
     * Delete all expenses from the table.
     * Used to clear the cache on logout or full refresh.
     */
    @Query("DELETE FROM expense_table")
    suspend fun deleteAllExpenses()

    /**
     * Get total expense amount for a specific category.
     */
    @Query("SELECT SUM(amount) FROM expense_table WHERE category = :category")
    suspend fun getTotalByCategory(category: String): Double?

    /**
     * Get all expenses within a date range.
     */
    @Query("SELECT * FROM expense_table WHERE dateTimestamp BETWEEN :startTime AND :endTime ORDER BY dateTimestamp DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<LocalExpense>>
}
