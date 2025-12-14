package com.example.finwise.data.repository

import android.util.Log
import com.example.finwise.api.ApiService
import com.example.finwise.data.local.dao.UserDao
import com.example.finwise.data.local.entity.LocalUser
import com.example.finwise.data.toLocalUser
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing User data.
 * Mediate between local database (UserDao) and remote API (ApiService).
 * Follows "Single Source of Truth" - UI observes DB, Repo updates DB from API.
 */
class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService
) {

    /**
     * Source of truth: Flow from the local database.
     * The UI should observe this.
     */
    val currentUser: Flow<LocalUser?> = userDao.getCurrentUser()

    /**
     * Refresh user profile from the API and update the local cache.
     * Handles network errors gracefully (logs them without crashing).
     */
    suspend fun refreshUserProfile(email: String) {
        try {
            // 1. Fetch from API
            val userProfile = apiService.getUserDetails(email)

            // 2. Map to Local Entity
            val localUser = userProfile.toLocalUser(email)

            // 3. Insert into Database (triggers Flow update)
            userDao.insertUser(localUser)
            
            Log.d("UserRepository", "User profile refreshed successfully")

        } catch (e: Exception) {
            // Log error but don't crash - app continues with cached data
            Log.e("UserRepository", "Failed to refresh user profile", e)
        }
    }
    
    /**
     * Clear user data on logout.
     */
    suspend fun clearUserData() {
        userDao.deleteAllUsers()
    }
}
