package com.example.myapplication.receiver

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
// --- ADDED IMPORTS FOR DATABASE ---
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.model.ThreatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

                    // 1. Show the Notification (Your existing logic)
                    showThreatNotification(context, sender, fakeConfidence)

                    // 2. Save to Local Database (The "Documenting" part)
                    saveThreatToLocalDatabase(context, sender, messageBody)

                } else {
                    Toast.makeText(context, "Safe message from $sender", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- NEW: LOGIC TO SAVE THREAT TO LOCAL MACHINE ---
    private fun saveThreatToLocalDatabase(context: Context, sender: String, body: String) {
        val db = AppDatabase.getDatabase(context)
        val currentTime = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date())

        val newThreat = ThreatEntity(
            sender = sender,
            snippet = body,
            time = currentTime
        )

        // We use Dispatchers.IO because we are writing to the local storage
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.threatDao().insertThreat(newThreat)
                Log.d("PhishGuard_DB", "Threat successfully documented in local DB")
            } catch (e: Exception) {
                Log.e("PhishGuard_DB", "Failed to save threat: ${e.message}")
            }
        }
    }

    private fun showThreatNotification(context: Context, sender: String, percentage: Int) {
        val channelId = "phishguard_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for detected SMS scams"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ PhishGuard Alert")
            .setContentText("Scam blocked from $sender ($percentage% confidence)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(sender.hashCode(), notification)
    }
}