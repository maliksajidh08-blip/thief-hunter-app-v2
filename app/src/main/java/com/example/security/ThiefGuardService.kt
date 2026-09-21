package com.example.security

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Android Foreground Service for continuous anti-theft monitoring in background.
 */
class ThiefGuardService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main)

    lateinit var sensorManager: SensorSecurityManager
        private set
    lateinit var sirenEngine: AlarmSirenEngine
        private set
    lateinit var notificationHelper: SecurityNotificationHelper
        private set
    lateinit var locationTracker: OfflineLocationTrackerEngine
        private set

    private var telemetryCollectJob: Job? = null

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): ThiefGuardService = this@ThiefGuardService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = SensorSecurityManager(this)
        sirenEngine = AlarmSirenEngine(this)
        notificationHelper = SecurityNotificationHelper(this)
        locationTracker = OfflineLocationTrackerEngine(this, scope)

        sensorManager.onTriggerAlarm = { reason ->
            sirenEngine.startSiren(reason.patternKey)
            notificationHelper.showTriggerNotification(
                reason.title,
                "Security perimeter breached: ${reason.title} at ${System.currentTimeMillis()}"
            )
            locationTracker.setDeviceStolen(true)
        }

        // Start Foreground Service notification immediately
        val notif = notificationHelper.buildForegroundNotification(6, "Anti-Theft Shield Armed & Monitoring")
        startForeground(SecurityNotificationHelper.SERVICE_NOTIFICATION_ID, notif)
        _isServiceRunning.value = true

        telemetryCollectJob = scope.launch {
            sensorManager.telemetry.collect { tele ->
                val armed = sensorManager.isArmed.value
                val status = if (armed) "Perimeter Guard Armed • Battery ${tele.batteryPct}%" else "Standby Guard Ready"
                val updatedNotif = notificationHelper.buildForegroundNotification(
                    activeSensorsCount = if (armed) 6 else 0,
                    statusText = status
                )
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(SecurityNotificationHelper.SERVICE_NOTIFICATION_ID, updatedNotif)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundGuard()
                stopSelf()
            }
            ACTION_DISARM -> {
                disarmAndStopAlarm()
            }
        }
        return START_STICKY
    }

    fun disarmAndStopAlarm() {
        sirenEngine.stopSiren()
        notificationHelper.cancelTriggerNotification()
        sensorManager.disarmSystem()
    }

    private fun stopForegroundGuard() {
        _isServiceRunning.value = false
        disarmAndStopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        telemetryCollectJob?.cancel()
        locationTracker.stopTracking()
        sensorManager.onDestroy()
        sirenEngine.stopSiren()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_SERVICE = "com.example.security.ACTION_STOP_SERVICE"
        const val ACTION_DISARM = "com.example.security.ACTION_DISARM"
    }
}
