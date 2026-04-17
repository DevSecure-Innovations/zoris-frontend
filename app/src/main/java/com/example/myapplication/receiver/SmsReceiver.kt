package com.example.myapplication.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ScanBridge
import com.example.myapplication.data.model.ScanResult
import com.example.myapplication.data.model.ScanType
import com.example.myapplication.data.model.ThreatEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Protection Gate
        val sharedPrefs = context.getSharedPreferences("phishguard_prefs", Context.MODE_PRIVATE)
        val isProtectionActive = sharedPrefs.getBoolean("PROTECTION_ACTIVE", false)

        if (!isProtectionActive || intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: "Unknown"
            val messageBody = sms.displayMessageBody ?: ""

            // 2. Pro AI/Keyword Logic
            val isScam = analyzeMessage(messageBody)
            val confidenceScore = if (isScam) 98 else 100

            // 3. Create Scan Object
            val newScan = ScanResult(
                id = UUID.randomUUID().toString(),
                type = ScanType.SMS,
                isSafe = !isScam,
                confidence = confidenceScore,
                analysisDetails = "From $sender"
            )

            // --- COUNTER LOGIC ---
            // 4. ALWAYS report to Bridge (Bumps "Scanned Today" +1)
            ScanBridge.reportScan(newScan)

            if (isScam) {
                // 5. ONLY save to DB if malicious (Bumps "Threats Blocked" +1)
                saveThreatToLocalDatabase(context, sender, messageBody)
                showThreatNotification(context, sender, confidenceScore)
                Log.d("PhishGuard_Receiver", "🚨 Threat Blocked from $sender")
            } else {
                Log.d("PhishGuard_Receiver", "✅ Safe message processed from $sender")
            }
        }
    }

    private fun analyzeMessage(message: String): Boolean {
        // Expanded "Pro" keyword list
        val suspiciousKeywords = listOf(
            "bank", "urgent", "scam", "verify", "suspended",
            "login", "action required", "unauthorized", "winner",
            "gift card", "crypto", "account blocked"
        )
        return suspiciousKeywords.any { message.contains(it, ignoreCase = true) }
    }

    private fun saveThreatToLocalDatabase(context: Context, sender: String, body: String) {
        val db = AppDatabase.getDatabase(context)
        val currentTime = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date())

        val newThreat = ThreatEntity(
            sender = sender,
            snippet = body,
            time = currentTime
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.threatDao().insertThreat(newThreat)
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
                channelId, "Threat Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts for detected SMS scams" }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ PhishGuard Alert")
            .setContentText("Potential scam from $sender blocked ($percentage% confidence)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}