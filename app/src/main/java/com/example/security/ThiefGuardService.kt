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
    lateinit var batteryOptimizer: BatteryOptimizer
        private set

    private var telemetryCollectJob: Job? = null
    private var lastNotifBatteryPct: Int = -1
    private var lastNotifArmedState: Boolean = false
    private var lastNotifTimeMs: Long = 0L

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
        locationTracker.startTracking()
        batteryOptimizer = BatteryOptimizer.getInstance(this)

        sensorManager.onTriggerAlarm = { reason ->
            if (sensorManager.isArmed.value) {
                sirenEngine.startSiren(reason.patternKey)
                notificationHelper.showTriggerNotification(
                    reason.title,
                    "Security alert: ${reason.title}. Master PIN required to disarm."
                )
                locationTracker.setDeviceStolen(true)
            }
        }

        // Start Foreground Service notification immediately
        try {
            val notif = notificationHelper.buildForegroundNotification(6, "Thief Hunter is Active • Background Protection & Live Location")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        SecurityNotificationHelper.SERVICE_NOTIFICATION_ID,
                        notif,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (se: SecurityException) {
                    android.util.Log.w("ThiefGuardService", "Fallback startForeground: ${se.message}")
                    startForeground(SecurityNotificationHelper.SERVICE_NOTIFICATION_ID, notif)
                }
            } else {
                startForeground(SecurityNotificationHelper.SERVICE_NOTIFICATION_ID, notif)
            }
            _isServiceRunning.value = true
            com.example.data.AppPreferences.setGuardServiceActive(this, true)
        } catch (e: Exception) {
            android.util.Log.e("ThiefGuardService", "Failed to startForeground: ${e.message}", e)
        }

        telemetryCollectJob = scope.launch {
            sensorManager.telemetry.collect { tele ->
                val armed = sensorManager.isArmed.value
                val now = System.currentTimeMillis()

                // Battery Saver: Only post notification update if armed state changes, battery changes by >=2%, or 30s elapsed
                val shouldUpdate = (armed != lastNotifArmedState) ||
                        (Math.abs(tele.batteryPct - lastNotifBatteryPct) >= 2) ||
                        (now - lastNotifTimeMs >= 30_000L)

                if (shouldUpdate) {
                    lastNotifArmedState = armed
                    lastNotifBatteryPct = tele.batteryPct
                    lastNotifTimeMs = now

                    val mode = batteryOptimizer.currentMode.value
                    val night = batteryOptimizer.isNightTimeWindow()
                    val thermal = batteryOptimizer.batteryThermalInfo.value

                    val status = buildString {
                        if (armed) {
                            append("Perimeter Guard Armed • ${tele.batteryPct}%")
                            if (night) append(" • 🌙 Night Sleep")
                        } else {
                            append("Standby Guard Ready • ${tele.batteryPct}%")
                        }
                        if (mode == com.example.data.BatteryMode.ULTRA_LOW) append(" • ⚡ Ultra Saver")
                        if (thermal.isThermalThrottled) append(" • 🌡️ Cooling")
                    }

                    val updatedNotif = notificationHelper.buildForegroundNotification(
                        activeSensorsCount = if (armed) 6 else 0,
                        statusText = status
                    )
                    val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(SecurityNotificationHelper.SERVICE_NOTIFICATION_ID, updatedNotif)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                com.example.data.AppPreferences.setGuardServiceActive(this, false)
                stopForegroundGuard()
                stopSelf()
            }
            ACTION_DISARM -> {
                com.example.data.AppPreferences.setSystemArmed(this, false)
                disarmAndStopAlarm()
            }
            ACTION_ARM -> {
                com.example.data.AppPreferences.setSystemArmed(this, true)
                val delay = com.example.data.AppPreferences.getArmingDelaySeconds(this)
                val sens = com.example.data.AppPreferences.getMotionSensitivity(this)
                sensorManager.startArmingSequence(delay, GuardConfig(motionSensitivity = sens))
            }
            ACTION_OPTIMIZE_BATTERY -> {
                scope.launch {
                    batteryOptimizer.runMemoryAndDbCleanup(this@ThiefGuardService)
                }
            }
        }
        return START_STICKY
    }

    fun disarmAndStopAlarm() {
        sirenEngine.stopSiren()
        AlarmSirenEngine.haltAllSirens(this)
        notificationHelper.cancelTriggerNotification()
        sensorManager.disarmSystem()
        sensorManager.clearTrigger()
    }

    private fun stopForegroundGuard() {
        com.example.data.AppPreferences.setGuardServiceActive(this, false)
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
        const val ACTION_ARM = "com.example.security.ACTION_ARM"
        const val ACTION_OPTIMIZE_BATTERY = "com.example.security.ACTION_OPTIMIZE_BATTERY"
    }
}
