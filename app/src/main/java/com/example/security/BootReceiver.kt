package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.AppPreferences

/**
 * BootReceiver restarts the ThiefGuardService and background protection on:
 * 1. Device Boot (BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, QUICKBOOT)
 * 2. App Update (MY_PACKAGE_REPLACED)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action
        Log.i(TAG, "Received broadcast action: $action")

        val isAutoStartEnabled = AppPreferences.isGuardServiceActive(context)
        if (!isAutoStartEnabled) {
            Log.d(TAG, "Guard service auto-start is disabled in preferences.")
            return
        }

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Starting ThiefGuardService in foreground from boot/update receiver.")
                try {
                    val serviceIntent = Intent(context, ThiefGuardService::class.java).apply {
                        if (AppPreferences.isSystemArmed(context)) {
                            this.action = ThiefGuardService.ACTION_ARM
                        }
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start ThiefGuardService on boot: ${e.message}", e)
                }
            }
        }
    }
}
