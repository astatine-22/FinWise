package com.example.finwise

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility object to manage all FinWise notifications
 */
object NotificationHelper {

    // Notification Channels
    private const val CHANNEL_ID_ALERTS = "finwise_alerts"
    private const val CHANNEL_NAME_ALERTS = "Budget Alerts"
    
    // Notification IDs
    private const val NOTIFICATION_ID_TRANSACTION = 1001
    private const val NOTIFICATION_ID_BUDGET_BAR = 1002

    /**
     * Create all necessary notification channels (Android O+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Budget Alerts Channel (High Importance for transaction alerts)
            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget alerts and transactions"
            }
            
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    /**
     * Show instant transaction alert notification
     * @param context Application context
     * @param amount Transaction amount
     * @param merchant Merchant/sender name
     */
    fun showTransactionNotification(context: Context, amount: Double, merchant: String) {
        createNotificationChannels(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent to open MainActivity when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Transaction Detected")
            .setContentText("Spent ₹%.2f at %s".format(amount, merchant))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A transaction of ₹%.2f was detected at %s. Your budget has been updated.".format(amount, merchant)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_TRANSACTION, notification)
    }

    /**
     * Show expense detected notification with categorization
     */
    fun showExpenseNotification(context: Context, amount: Double, category: String) {
        createNotificationChannels(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            1003, // Unique ID for this notification type
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Expense Detected: ₹%.2f".format(amount))
            .setContentText("Categorized as: $category (Tap to save)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Categorized as: $category (Tap to save)"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID_TRANSACTION, notification)
    }

    /**
     * Update persistent budget notification bar
     * Shows as an ongoing notification with progress bar
     * @param context Application context
     * @param totalSpent Total amount spent this month
     * @param budgetLimit Monthly budget limit
     */
    fun updateBudgetNotification(context: Context, totalSpent: Double, budgetLimit: Double) {
        createNotificationChannels(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Calculate percentage
        val percentage = if (budgetLimit > 0) ((totalSpent / budgetLimit) * 100).toInt() else 0
        
        // Get current month name
        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
        
        // Create intent to open MainActivity when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build persistent notification with progress bar
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$currentMonth Budget")
            .setContentText("Spent ₹%.0f of ₹%.0f (%d%%)".format(totalSpent, budgetLimit, percentage))
            .setProgress(100, percentage, false) // Progress bar: max=100, current=percentage
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority to avoid sound/vibration
            .setOngoing(true) // Make it persistent (cannot be swiped away)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // Don't alert again when updating
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BUDGET_BAR, notification)
    }

    /**
     * Clear the persistent budget notification
     * @param context Application context
     */
    fun clearBudgetNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_BUDGET_BAR)
    }

    /**
     * Clear all notifications
     * @param context Application context
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}
