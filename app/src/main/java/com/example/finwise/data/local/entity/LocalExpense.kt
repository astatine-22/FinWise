package com.example.finwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a locally cached expense record.
 * Includes an isSynced flag for future offline sync capability.
 */
@Entity(tableName = "expense_table")
data class LocalExpense(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,         // Auto-generated local primary key
    val backendId: String? = null, // Nullable, links to server ID once synced
    val amount: Double,
    val category: String,
    val description: String,
    val dateTimestamp: Long,       // Store dates as epoch millis for efficiency
    val isSynced: Boolean = true   // Crucial flag for future sync logic
)
