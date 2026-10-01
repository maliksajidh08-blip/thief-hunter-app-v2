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
import android.util.Log
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
    val speed: Float = 0.0f,
    val isInPocket: Boolean = false,
    val isWalkingFiltered: Boolean = false,
    val liftDetected: Boolean = false,
    val smartPocketModeActive: Boolean = true,
    val deviceContext: DeviceContext = DeviceContext.TABLE,
    val movementActivity: MovementActivity = MovementActivity.STATIONARY,
    val isInGracePeriod: Boolean = false,
    val aiDecision: AIDecisionResult? = null
)

data class GuardConfig(
    val pocketDetectionEnabled: Boolean = true,
    val motionDetectionEnabled: Boolean = true,
    val chargerUnplugEnabled: Boolean = true,
    val usbConnectionEnabled: Boolean = true,
    val liveGpsTrackingEnabled: Boolean = true,
    val intruderSelfieEnabled: Boolean = true,
    val motionSensitivity: Float = 2.0f, // 1: Low, 2: Medium (Default), 3: High
    val armingDelaySeconds: Int = 30
)

enum class TriggerReason(val title: String, val patternKey: String) {
    POCKET_REMOVAL("Pocket Extraction Theft Detected", "POCKET_REMOVAL"),
    HAND_GRAB_DETECTED("Hand Touch / Pocket Snatch Detected", "POCKET_REMOVAL"),
    MOTION_DETECTED("Unauthorized Motion Detected", "MOTION_DETECTION"),
    CHARGER_UNPLUGGED("Charger Disconnected", "CHARGER_UNPLUG"),
    USB_CONNECTED("Unauthorized USB Connected", "USB_CONNECTION"),
    INTRUDER_FAILED_PIN("Failed Master PIN Attempt", "EMERGENCY_ALARM"),
    SLEEP_TOUCH_BREACH("Sleep Guard: Unauthorized Touch While Sleeping", "EMERGENCY_ALARM"),
    MANUAL_PANIC("Manual Emergency Siren", "EMERGENCY_ALARM")
}

class SensorSecurityManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val batteryOptimizer = BatteryOptimizer.getInstance(context)
    val behaviorTrainer = BehaviorTrainer(context)
    val aiEngine = AIBehaviorEngine(context, behaviorTrainer)
    val contextDetector = ContextDetector(context)
    val movementAnalyzer = MovementAnalyzer(context, behaviorTrainer)

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

    // Last recorded touch parameters for AI inference
    private var lastTouchFingerSize: Float? = null
    private var lastTouchPressure: Float? = null
    private var lastTouchDurationMs: Float? = null
    private var lastTouchSwipeSpeed: Float? = null

    private var initialArmedLight: Float? = null
    private var initialArmedProximityNear: Boolean = false
    private var wasPluggedInWhenArmed: Boolean = false
    private var wasUsbConnectedWhenArmed: Boolean = false
    private var wasInPocketAtArming: Boolean = false

    private var lastTotalAccel: Float = 9.8f
    private var lastAccelY: Float = 0f
    private var lastAccelZ: Float = 9.8f
    private var lastInclination: Float = 0f
    private var lastSensorTimestamp: Long = 0L
    private var lastProcessedSampleTimestamp: Long = 0L
    private var areSensorsRegistered: Boolean = false
    private var currentGuardConfig: GuardConfig = GuardConfig()

    private var countdownJob: Job? = null
    private var locationCallback: LocationCallback? = null

    var onTriggerAlarm: ((TriggerReason) -> Unit)? = null
    var onPhoneLifted: (() -> Unit)? = null

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
        movementAnalyzer.trainer = behaviorTrainer
        registerSensors()
        registerPowerReceiver()
        batteryOptimizer.onSensorsThermalShutdown = { shutdown ->
            if (shutdown) {
                unregisterSensors()
            } else if (_isArmed.value) {
                registerSensors()
            }
        }
    }

    /**
     * Records real-time touch parameters from UI to train Day 1-2 Touch Pattern & Day 5-6 Grip Pattern.
     */
    fun recordTouchEvent(
        fingerSize: Float,
        pressure: Float,
        durationMs: Float,
        swipeSpeed: Float
    ) {
        lastTouchFingerSize = fingerSize
        lastTouchPressure = pressure
        lastTouchDurationMs = durationMs
        lastTouchSwipeSpeed = swipeSpeed

        contextDetector.notifyUserTouchInteraction()
        movementAnalyzer.notifyTouchDetected()

        behaviorTrainer.recordTouchSample(fingerSize, pressure, durationMs, swipeSpeed)
        behaviorTrainer.recordGripSample(
            fingerCount = 1.0f,
            gripPositionRatio = 0.65f,
            pressureConsistency = 0.88f,
            naturalMovementVariance = 0.12f
        )
    }

    fun registerSensors() {
        if (areSensorsRegistered) return
        if (!batteryOptimizer.shouldSensorsBeActive(_isArmed.value)) return

        val delay = batteryOptimizer.getSensorDelay()
        sensorManager?.let { sm ->
            proximitySensor?.let { sm.registerListener(this, it, delay) }
            lightSensor?.let { sm.registerListener(this, it, delay) }
            accelerometer?.let { sm.registerListener(this, it, delay) }
        }
        areSensorsRegistered = true
    }

    fun unregisterSensors() {
        if (!areSensorsRegistered) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        areSensorsRegistered = false
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

        currentGuardConfig = config
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
            movementAnalyzer.notifySystemArmed()

            registerSensors()

            // Snapshot baseline at moment of arming
            initialArmedProximityNear = _telemetry.value.isProximityNear
            initialArmedLight = _telemetry.value.lightLux
            wasPluggedInWhenArmed = _telemetry.value.isPowerConnected
            wasUsbConnectedWhenArmed = _telemetry.value.isUsbConnected
            wasInPocketAtArming = initialArmedProximityNear && ((initialArmedLight ?: 100f) < 45f)

            _telemetry.update {
                it.copy(
                    isInPocket = wasInPocketAtArming,
                    isWalkingFiltered = false
                )
            }

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
        if (batteryOptimizer.currentMode.value != com.example.data.BatteryMode.PERFORMANCE) {
            unregisterSensors()
        }
    }

    fun triggerPanic() {
        fireTrigger(TriggerReason.MANUAL_PANIC)
    }

    fun fireTrigger(reason: TriggerReason) {
        _activeTrigger.value = reason
        onTriggerAlarm?.invoke(reason)
    }

    fun clearTrigger() {
        _activeTrigger.value = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isNear = dist < 3.0f || (dist < maxRange && dist < 4.0f)
                val wasNear = _telemetry.value.isProximityNear

                _telemetry.update {
                    it.copy(
                        proximityCm = dist,
                        isProximityNear = isNear
                    )
                }

                // If user is actively touching screen, update context
                if (contextDetector.isUserRecentlyInteracting()) {
                    contextDetector.notifyUserTouchInteraction()
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                val isNear = _telemetry.value.isProximityNear

                _telemetry.update {
                    it.copy(
                        lightLux = lux,
                        isInPocket = isNear && (lux < 30f)
                    )
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                val deltaFromLast = Math.abs(total - lastTotalAccel)

                // Battery Saver rate limit: In Ultra Low, process accelerometer once every 5 seconds (unless violent grab)
                val throttleMs = batteryOptimizer.getSensorSampleThrottleMs()
                val isUrgentSpike = deltaFromLast > 4.5f
                if (throttleMs > 0 && !isUrgentSpike && (now - lastProcessedSampleTimestamp < throttleMs)) {
                    return
                }
                lastProcessedSampleTimestamp = now

                val isNear = _telemetry.value.isProximityNear
                val lightLux = _telemetry.value.lightLux

                // 1. Context detection (HAND, LEG, POCKET, TABLE, BAG)
                val detectedContext = contextDetector.update(
                    isProximityNear = isNear,
                    lightLux = lightLux,
                    accelX = x,
                    accelY = y,
                    accelZ = z,
                    totalAcceleration = total,
                    isWalkingMotion = movementAnalyzer.isWalkingPattern
                )

                // 2. Movement & Activity analysis (WALKING, RUNNING, LIFT, SNATCH, GRACE PERIOD)
                val motionResult = movementAnalyzer.analyze(
                    x = x,
                    y = y,
                    z = z,
                    totalAccel = total,
                    proximityNear = isNear,
                    wasProximityNear = initialArmedProximityNear,
                    lightLux = lightLux,
                    wasLightLux = initialArmedLight ?: lightLux,
                    deviceContext = detectedContext
                )

                // 3. Real-Time AI Decision Engine Inference
                val aiDecision = aiEngine.evaluate(
                    touchFingerSize = lastTouchFingerSize,
                    touchPressure = lastTouchPressure,
                    touchDurationMs = lastTouchDurationMs,
                    touchSwipeSpeed = lastTouchSwipeSpeed,
                    liftSpeedMs = motionResult.estimatedSpeedMs,
                    liftAngleDeg = motionResult.tiltAngleDelta,
                    rotationDeg = motionResult.rotationDeltaDeg,
                    gForceDelta = motionResult.gForceDelta,
                    isProximityNear = isNear,
                    lightLux = lightLux,
                    deviceContext = detectedContext
                )

                _telemetry.update {
                    it.copy(
                        accelX = x,
                        accelY = y,
                        accelZ = z,
                        totalAcceleration = total,
                        liftDetected = motionResult.isLiftTrigger,
                        isInPocket = detectedContext == DeviceContext.POCKET,
                        isWalkingFiltered = motionResult.activity == MovementActivity.WALKING,
                        deviceContext = detectedContext,
                        movementActivity = motionResult.activity,
                        isInGracePeriod = motionResult.isInGracePeriod,
                        aiDecision = aiDecision
                    )
                }

                if (_isArmed.value) {
                    // Check AI Decision Scenarios:
                    when (aiDecision.decision) {
                        AIDecisionAction.NO_ALARM -> {
                            // Scenario 1: Owner using phone or Scenario 2: Owner keeps phone on leg
                            // Alarm suppressed!
                        }

                        AIDecisionAction.IMMEDIATE_ALARM -> {
                            // Scenario 5: Thief snatches phone (>3 m/s or violent jerk)
                            if (!motionResult.isInGracePeriod && detectedContext != DeviceContext.HAND) {
                                fireTrigger(TriggerReason.HAND_GRAB_DETECTED)
                            }
                        }

                        AIDecisionAction.ALARM_AND_SMS -> {
                            // Scenario 4: Stranger picks up phone
                            if (!motionResult.isInGracePeriod) {
                                fireTrigger(TriggerReason.MOTION_DETECTED)
                            }
                        }

                        AIDecisionAction.WATCH_MODE -> {
                            // Scenario 3: Owner keeps phone on table / stowed
                            if (!motionResult.isInGracePeriod) {
                                if (currentGuardConfig.pocketDetectionEnabled && motionResult.isPocketExtractionTrigger) {
                                    fireTrigger(TriggerReason.POCKET_REMOVAL)
                                } else if (motionResult.isSnatchTrigger && detectedContext != DeviceContext.HAND) {
                                    fireTrigger(TriggerReason.HAND_GRAB_DETECTED)
                                } else if (currentGuardConfig.motionDetectionEnabled && motionResult.isLiftTrigger && detectedContext == DeviceContext.TABLE) {
                                    if (onPhoneLifted != null) {
                                        onPhoneLifted?.invoke()
                                    } else {
                                        fireTrigger(TriggerReason.MOTION_DETECTED)
                                    }
                                }
                            }
                        }
                    }
                }

                lastTotalAccel = total
                lastAccelY = y
                lastAccelZ = z
                val normZ = (z / total.coerceAtLeast(0.1f)).toDouble().coerceIn(-1.0, 1.0)
                lastInclination = Math.toDegrees(Math.acos(normZ)).toFloat()
                lastSensorTimestamp = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun startGpsTracking() {
        val isStolen = _activeTrigger.value != null
        val intervalMs = batteryOptimizer.getGpsIntervalMs(isStolen)
        if (intervalMs < 0) {
            stopGpsTracking()
            return
        }

        try {
            val priority = if (isStolen) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val locationRequest = LocationRequest.Builder(priority, intervalMs)
                .setMinUpdateIntervalMillis((intervalMs / 2).coerceAtLeast(5000L))
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        _telemetry.update {
                            it.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                speed = loc.speed * 3.6f
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
