package com.example.finwise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finwise.data.local.dao.ExpenseDao
import com.example.finwise.data.local.dao.UserDao
import com.example.finwise.data.local.entity.LocalExpense
import com.example.finwise.data.local.entity.LocalUser

/**
 * Room Database holder for FinWise app.
 * Implements the Singleton pattern to ensure only one database instance exists.
 * 
 * Entities:
 * - LocalUser: Cached user profile data
 * - LocalExpense: Cached expense records with sync flag
 * 
 * Version: 2 (Added profilePicture to LocalUser)
 */
@Database(
    entities = [LocalUser::class, LocalExpense::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get UserDao for user profile operations.
     */
    abstract fun userDao(): UserDao

    /**
     * Get ExpenseDao for expense/budget operations.
     */
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get the singleton database instance.
         * Uses double-checked locking for thread safety.
         * 
         * @param context Application context (avoid Activity context to prevent leaks)
         * @return The singleton AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finwise_database"
                )
                    // Enable destructive migrations for development
                    // TODO: Replace with proper migrations for production
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
