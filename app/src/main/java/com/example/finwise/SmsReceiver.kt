package com.example.finwise

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // TEST MODE: Allow triggering via ADB without a SIM card
        // adb shell am broadcast -a com.example.finwise.TEST_SMS --es "sender" "AX-HDFCB" --es "body" "Rs. 500 spent on Food"
        if (intent.action == "com.example.finwise.TEST_SMS") {
            val sender = intent.getStringExtra("sender") ?: return
            val messageBody = intent.getStringExtra("body") ?: return
            Log.d(TAG, "TEST SMS Received from: $sender")
            
            if (isValidBankSender(sender)) {
                processBankSms(context, messageBody)
            } else {
                Log.d(TAG, "Ignored test sender: $sender")
            }
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        for (pdu in pdus) {
                            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val format = bundle.getString("format")
                                SmsMessage.createFromPdu(pdu as ByteArray, format)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsMessage.createFromPdu(pdu as ByteArray)
                            }

                            val sender = smsMessage.displayOriginatingAddress
                            val messageBody = smsMessage.messageBody

                            Log.d(TAG, "SMS Received from: $sender")
                            
                            // Filter Senders: String length = 8 and contains "-"
                            if (isValidBankSender(sender)) {
                                processBankSms(context, messageBody)
                            } else {
                                Log.d(TAG, "Ignored sender: $sender")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS", e)
                }
            }
        }
    }

    private fun isValidBankSender(sender: String): Boolean {
        // Process messages from senders with length > 9 (ignoring normal mobile numbers)
        // This filters out personal mobile numbers while allowing bank/merchant sender IDs
        // Example: AX-HDFCBK, VM-AMAZON, etc.
        return sender.length > 9
    }

    private fun processBankSms(context: Context, messageBody: String) {
        val amount = extractAmount(messageBody)
        if (amount != null) {
            val category = MerchantHelper.getCategoryFromMessage(messageBody)
            val merchantName = MerchantHelper.getMerchantName(messageBody)
            
            // Check permission before notifying (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted. Cannot show notification.")
                    return
                }
            }

            // Create intent to open AddExpenseActivity with pre-filled data
            val baseIntent = Intent(context, AddExpenseActivity::class.java).apply {
                putExtra("EXTRA_AMOUNT", amount.toString())
                putExtra("EXTRA_TITLE", merchantName)
                putExtra("EXTRA_CATEGORY", category)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                baseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification based on category confidence
            val builder = NotificationCompat.Builder(context, "finwise_expenses")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Expense Detected")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // If category is uncategorized, add action buttons for quick category selection
            if (category == "Uncategorized") {
                builder.setContentText("New Expense at $merchantName. Select Category:")
                
                // Create action buttons for common categories
                val categories = listOf("Food", "Transport", "Shopping")
                val icons = listOf(R.drawable.ic_food, R.drawable.ic_transport, R.drawable.ic_shopping)
                
                categories.forEachIndexed { index, cat ->
                    val actionIntent = Intent(context, AddExpenseActivity::class.java).apply {
                        putExtra("EXTRA_AMOUNT", amount.toString())
                        putExtra("EXTRA_TITLE", merchantName)
                        putExtra("EXTRA_CATEGORY", cat)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    
                    val actionPendingIntent = PendingIntent.getActivity(
                        context,
                        System.currentTimeMillis().toInt() + index + 1, // Unique request code
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    builder.addAction(icons[index], cat, actionPendingIntent)
                }
            } else {
                // Standard notification for recognized categories
                builder.setContentText("Spent: ₹$amount ($category)")
            }

            try {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException showing notification", e)
            }
        } else {
            Log.d(TAG, "No amount found in message")
        }
    }

    private fun extractAmount(messageBody: String): Double? {
        // Regex for "Rs.", "INR", or "₹"
        // Case insensitive
        // Matches: Rs. 500, INR 500, ₹500, ₹ 500.00
        val regex = Regex("""(?i)(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{2})?)""")
        val match = regex.find(messageBody)
        
        return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
}
