package com.example.finwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing the locally cached user profile.
 * Stores essential user data for offline access.
 */
@Entity(tableName = "user_table")
data class LocalUser(
    @PrimaryKey
    val userId: String,           // Matches backend user ID
    val email: String,
    val displayName: String,
    val xp: Int = 0,
    val currentBudgetLimit: Double = 20000.0,
    val profilePicture: String? = null  // Base64 encoded profile picture
)

