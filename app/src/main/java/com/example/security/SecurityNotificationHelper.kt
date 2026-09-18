package com.example.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class SecurityNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val guardChannel = NotificationChannel(
                CHANNEL_GUARD_SERVICE,
                "Thief Guard Active Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of anti-theft sensors"
                setShowBadge(false)
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_SECURITY_ALARM,
                "Anti-Theft Emergency Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority emergency alarms when sensors are triggered"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(guardChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun buildForegroundNotification(activeSensorsCount: Int, statusText: String): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_GUARD_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Thief Hunter • Security Guard Active")
            .setContentText(statusText)
            .setSubText("$activeSensorsCount Sensors Armed")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun showTriggerNotification(triggerTitle: String, triggerDetails: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ THEFT ALERT: $triggerTitle")
            .setContentText(triggerDetails)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$triggerDetails. Master PIN required to disarm alarm."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    fun cancelTriggerNotification() {
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_GUARD_SERVICE = "thief_guard_service_channel"
        const val CHANNEL_SECURITY_ALARM = "thief_security_alarm_channel"
        const val SERVICE_NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002
    }
}
