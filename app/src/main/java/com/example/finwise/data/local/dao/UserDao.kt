package com.example.finwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finwise.data.local.entity.LocalUser
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LocalUser entity.
 * Provides database operations for user profile data.
 */
@Dao
interface UserDao {

    /**
     * Insert or update user. Uses REPLACE strategy to handle existing records.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: LocalUser)

    /**
     * Get the current logged-in user as a Flow for reactive updates.
     * Returns null if no user is cached.
     */
    @Query("SELECT * FROM user_table LIMIT 1")
    fun getCurrentUser(): Flow<LocalUser?>

    /**
     * Get user synchronously (non-Flow) for one-time reads.
     */
    @Query("SELECT * FROM user_table LIMIT 1")
    suspend fun getCurrentUserOnce(): LocalUser?

    /**
     * Delete all users from the table.
     * Called during logout to clear cached user data.
     */
    @Query("DELETE FROM user_table")
    suspend fun deleteAllUsers()
}
