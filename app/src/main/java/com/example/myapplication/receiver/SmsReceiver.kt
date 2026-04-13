package com.example.myapplication.receiver

// --- ALL REQUIRED IMPORTS ---
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.displayMessageBody ?: ""

                Log.d("PhishGuard_SMS", "Intercepted: $messageBody")

                // --- SIMULATING THE AI BACKEND ---
                val isSuspicious = messageBody.contains("bank", ignoreCase = true) ||
                        messageBody.contains("scam", ignoreCase = true)

                if (isSuspicious) {
                    val fakeConfidence = 98
                    showThreatNotification(context, sender, fakeConfidence)
                } else {
                    // Just for testing, show a toast for safe messages
                    Toast.makeText(context, "Safe message from $sender", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- BUILDS AND SHOWS THE ANDROID NOTIFICATION ---
    // Notice how this function is INSIDE the SmsReceiver class now!
    private fun showThreatNotification(context: Context, sender: String, percentage: Int) {
        val channelId = "phishguard_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Create Notification Channel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH // Drops down from the top
            ).apply {
                description = "Alerts for detected SMS scams"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Build the Notification UI
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Built-in warning triangle
            .setContentTitle("⚠️ PhishGuard Alert")
            .setContentText("Scam blocked from $sender ($percentage% confidence)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Clears when swiped
            .build()

        // 3. Fire the alert!
        notificationManager.notify(sender.hashCode(), notification)
    }
}