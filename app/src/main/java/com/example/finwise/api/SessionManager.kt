package com.example.finwise.api

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager handles secure storage and retrieval of authentication tokens
 * and user session data using SharedPreferences.
 * 
 * This class provides a centralized way to manage:
 * - JWT access tokens for API authentication
 * - User email for identification
 * - Session lifecycle (login/logout)
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "FinWiseSecurePrefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        @Volatile
        private var INSTANCE: SessionManager? = null

        /**
         * Get singleton instance of SessionManager.
         * Must be initialized with application context first.
         */
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Save the JWT authentication token.
     * @param token The JWT access token received from the server.
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    /**
     * Retrieve the stored JWT authentication token.
     * @return The stored token, or null if no token is saved.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Save user email for identification.
     * @param email The user's email address.
     */
    fun saveUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Retrieve the stored user email.
     * @return The stored email, or null if not saved.
     */
    fun fetchUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Save user's display name.
     * @param name The user's name.
     */
    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * Retrieve the stored user name.
     * @return The stored name, or null if not saved.
     */
    fun fetchUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    /**
     * Save complete session data after successful login.
     * @param token JWT access token
     * @param email User's email
     * @param name User's display name
     */
    fun saveSession(token: String, email: String, name: String?) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_EMAIL, email)
            name?.let { putString(KEY_USER_NAME, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Check if a user is currently logged in (has valid session).
     * @return True if user is logged in, false otherwise.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && fetchAuthToken() != null
    }

    /**
     * Clear all session data (logout).
     * This removes the token, email, and all session-related data.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
