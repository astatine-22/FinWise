package com.example.finwise.data

import com.example.finwise.api.ExpenseResponse
import com.example.finwise.api.UserProfile
import com.example.finwise.data.local.entity.LocalExpense
import com.example.finwise.data.local.entity.LocalUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// DATA MAPPERS (Network -> Local)
// ============================================================================

/**
 * Convert Network UserProfile to LocalUser entity.
 * Note: UserProfile from API doesn't contain email or ID, so we pass email explicitly.
 * We use email as the userId for local caching simplicity in this phase.
 */
fun UserProfile.toLocalUser(email: String): LocalUser {
    return LocalUser(
        userId = email, // Using email as unique ID for local cache
        email = email,
        displayName = this.name,
        xp = this.xp,
        currentBudgetLimit = 20000.0, // Default, or fetch from preferences if available
        profilePicture = this.profile_picture
    )
}

/**
 * Convert Network ExpenseResponse to LocalExpense entity.
 * Handles date string parsing to timestamp.
 */
fun ExpenseResponse.toLocalExpense(): LocalExpense {
    val dateMillis = try {
        // Assuming API returns date in "yyyy-MM-dd" format
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.parse(this.date)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return LocalExpense(
        backendId = this.id.toString(),
        amount = this.amount.toDouble(),
        category = this.category,
        description = this.title,
        dateTimestamp = dateMillis,
        isSynced = true // Data coming from server is already synced
    )
}

/**
 * Convert a list of network expenses to a list of local entities.
 */
fun List<ExpenseResponse>.toLocalExpenses(): List<LocalExpense> {
    return this.map { it.toLocalExpense() }
}
