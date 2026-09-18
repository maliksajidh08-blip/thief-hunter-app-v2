package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class SensorTelemetry(
    val proximityCm: Float = 5f,
    val isProximityNear: Boolean = false,
    val lightLux: Float = 250f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val totalAcceleration: Float = 9.8f,
    val isPowerConnected: Boolean = false,
    val isUsbConnected: Boolean = false,
    val batteryPct: Int = 100,
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f
)

data class GuardConfig(
    val pocketDetectionEnabled: Boolean = true,
    val motionDetectionEnabled: Boolean = true,
    val chargerUnplugEnabled: Boolean = true,
    val usbConnectionEnabled: Boolean = true,
    val liveGpsTrackingEnabled: Boolean = true,
    val intruderSelfieEnabled: Boolean = true,
    val motionSensitivity: Float = 3.0f, // 1 to 5 (lower number = higher threshold needed)
    val armingDelaySeconds: Int = 3
)

enum class TriggerReason(val title: String, val patternKey: String) {
    POCKET_REMOVAL("Pocket Removal Detected", "POCKET_REMOVAL"),
    MOTION_DETECTED("Unauthorized Motion Detected", "MOTION_DETECTION"),
    CHARGER_UNPLUGGED("Charger Disconnected", "CHARGER_UNPLUG"),
    USB_CONNECTED("Unauthorized USB Connected", "USB_CONNECTION"),
    INTRUDER_FAILED_PIN("Failed Master PIN Attempt", "EMERGENCY_ALARM"),
    MANUAL_PANIC("Manual Emergency Siren", "EMERGENCY_ALARM")
}

class SensorSecurityManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    private val _isArmed = MutableStateFlow(false)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _isArmingCountdown = MutableStateFlow(false)
    val isArmingCountdown: StateFlow<Boolean> = _isArmingCountdown.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(0)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _activeTrigger = MutableStateFlow<TriggerReason?>(null)
    val activeTrigger: StateFlow<TriggerReason?> = _activeTrigger.asStateFlow()

    private var initialArmedLight: Float? = null
    private var initialArmedProximityNear: Boolean = false
    private var wasPluggedInWhenArmed: Boolean = false
    private var wasUsbConnectedWhenArmed: Boolean = false

    private var countdownJob: Job? = null
    private var locationCallback: LocationCallback? = null

    var onTriggerAlarm: ((TriggerReason) -> Unit)? = null

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _telemetry.update { it.copy(isPowerConnected = true) }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _telemetry.update { it.copy(isPowerConnected = false) }
                    if (_isArmed.value && wasPluggedInWhenArmed) {
                        fireTrigger(TriggerReason.CHARGER_UNPLUGGED)
                    }
                }
                "android.hardware.usb.action.USB_STATE" -> {
                    val connected = intent.extras?.getBoolean("connected") ?: false
                    _telemetry.update { it.copy(isUsbConnected = connected) }
                    if (_isArmed.value && !wasUsbConnectedWhenArmed && connected) {
                        fireTrigger(TriggerReason.USB_CONNECTED)
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level != -1 && scale > 0) (level * 100) / scale else 100
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val isPlugged = plugged != 0
                    val isUsb = plugged == BatteryManager.BATTERY_PLUGGED_USB
                    _telemetry.update {
                        it.copy(
                            batteryPct = pct,
                            isPowerConnected = isPlugged,
                            isUsbConnected = isUsb
                        )
                    }
                }
            }
        }
    }

    init {
        registerSensors()
        registerPowerReceiver()
    }

    private fun registerSensors() {
        sensorManager?.let { sm ->
            proximitySensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            lightSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    private fun registerPowerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("android.hardware.usb.action.USB_STATE")
        }
        try {
            context.registerReceiver(powerReceiver, filter)
        } catch (_: Exception) {}
    }

    fun startArmingSequence(delaySec: Int, config: GuardConfig) {
        if (_isArmed.value || _isArmingCountdown.value) return

        _isArmingCountdown.value = true
        _countdownRemaining.value = delaySec

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in delaySec downTo 1) {
                _countdownRemaining.value = i
                delay(1000)
            }
            _isArmingCountdown.value = false
            _isArmed.value = true
            _countdownRemaining.value = 0

            // Snapshot baseline at moment of arming
            initialArmedProximityNear = _telemetry.value.isProximityNear
            initialArmedLight = _telemetry.value.lightLux
            wasPluggedInWhenArmed = _telemetry.value.isPowerConnected
            wasUsbConnectedWhenArmed = _telemetry.value.isUsbConnected

            if (config.liveGpsTrackingEnabled) {
                startGpsTracking()
            }
        }
    }

    fun disarmSystem() {
        countdownJob?.cancel()
        _isArmingCountdown.value = false
        _isArmed.value = false
        _activeTrigger.value = null
        stopGpsTracking()
    }

    fun fireTrigger(reason: TriggerReason) {
        if (_activeTrigger.value != null) return // Already triggered
        _activeTrigger.value = reason
        onTriggerAlarm?.invoke(reason)
    }

    fun clearTrigger() {
        _activeTrigger.value = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isNear = dist < maxRange.coerceAtMost(5f)
                _telemetry.update { it.copy(proximityCm = dist, isProximityNear = isNear) }

                // Pocket removal check: phone was in pocket (Near/Dark), now removed (Far)
                if (_isArmed.value && initialArmedProximityNear && !isNear) {
                    fireTrigger(TriggerReason.POCKET_REMOVAL)
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                _telemetry.update { it.copy(lightLux = lux) }

                // If in dark pocket (< 10 lux) and suddenly exposed to bright light (> 50 lux)
                if (_isArmed.value && initialArmedProximityNear && lux > 60f) {
                    fireTrigger(TriggerReason.POCKET_REMOVAL)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                _telemetry.update {
                    it.copy(
                        accelX = x,
                        accelY = y,
                        accelZ = z,
                        totalAcceleration = total
                    )
                }

                // Motion detection check: if armed and delta from earth gravity (9.8m/s²) exceeds threshold
                if (_isArmed.value) {
                    val delta = Math.abs(total - 9.8f)
                    // Sensitivity 1..5: sensitivity 5 triggers at delta > 1.2, sensitivity 1 triggers at delta > 4.5
                    val threshold = (6.0f - 3.0f).coerceIn(1.2f, 5.0f)
                    if (delta > threshold) {
                        fireTrigger(TriggerReason.MOTION_DETECTED)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun startGpsTracking() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1500)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        _telemetry.update {
                            it.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                speed = loc.speed * 3.6f // km/h
                            )
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Location permission may not be granted yet
        }
    }

    fun stopGpsTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    fun onDestroy() {
        stopGpsTracking()
        try {
            sensorManager?.unregisterListener(this)
            context.unregisterReceiver(powerReceiver)
        } catch (_: Exception) {}
    }
}
