package com.example.security

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

object SmsAlertHelper {

    fun sendSms(
        context: Context,
        phoneNumber: String,
        message: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            onError("Emergency contact phone is empty")
            return
        }

        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null)
            }
            Log.d("SmsAlertHelper", "SMS dispatched to $cleanPhone: $message")
            onSuccess()
        } catch (e: SecurityException) {
            Log.e("SmsAlertHelper", "SMS permission missing", e)
            onError("SMS permission missing or denied: ${e.message}")
        } catch (e: Exception) {
            Log.e("SmsAlertHelper", "Failed to dispatch SMS", e)
            onError("Failed to send SMS alert: ${e.message}")
        }
    }
}
