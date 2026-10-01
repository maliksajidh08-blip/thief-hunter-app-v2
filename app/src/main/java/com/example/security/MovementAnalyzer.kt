package com.example.security

import android.content.Context
import android.util.Log
import com.example.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

enum class MovementActivity(val displayName: String, val shouldIgnoreMovement: Boolean) {
    STATIONARY("Stationary (Resting)", false),
    WALKING("Walking (Filtered)", true),
    RUNNING("Running (Filtered)", true),
    IN_HAND_USE("In-Hand Use (Ignored)", true),
    SUSPICIOUS_LIFT("Lift Motion Detected", false),
    VIOLENT_SNATCH("Snatch Breach Detected", false)
}

data class MotionAnalysisResult(
    val activity: MovementActivity,
    val isLiftTrigger: Boolean,
    val isSnatchTrigger: Boolean,
    val isPocketExtractionTrigger: Boolean,
    val isInGracePeriod: Boolean,
    val gForceDelta: Float,
    val tiltAngleDelta: Float,
    val estimatedSpeedMs: Float,
    val explanation: String
)

/**
 * MovementAnalyzer performs intelligent movement analysis, activity recognition,
 * grace period verification, owner movement pattern learning, and sensor fusion transition checks.
 */
class MovementAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "MovementAnalyzer"

        // Threshold specifications:
        // G-Force delta minimum 2.0+ m/s² (up from 0.5)
        const val MIN_GFORCE_LIFT_THRESHOLD = 2.0f
        // Tilt angle delta minimum 20°+ (up from 5°)
        const val MIN_TILT_LIFT_THRESHOLD_DEG = 20.0f
        // Speed minimum 0.5+ m/s (up from 0.1)
        const val MIN_SPEED_THRESHOLD_MS = 0.5f

        // Violent snatch jerk threshold
        const val SNATCH_JERK_THRESHOLD = 5.5f

        // Grace periods:
        const val ARMING_GRACE_PERIOD_MS = 5000L   // 5 seconds after arming
        const val TOUCH_GRACE_PERIOD_MS = 3000L    // 3 seconds after touch
    }

    private val _currentActivity = MutableStateFlow(MovementActivity.STATIONARY)
    val currentActivity: StateFlow<MovementActivity> = _currentActivity.asStateFlow()

    private var armingTimestampMs: Long = 0L
    private var lastTouchTimestampMs: Long = 0L

    private var lastTotalAccel: Float = 9.8f
    private var lastAccelX: Float = 0f
    private var lastAccelY: Float = 0f
    private var lastAccelZ: Float = 9.8f
    private var lastInclinationDeg: Float = 0f
    private var lastTimestampMs: Long = 0L

    // Step / walking oscillation tracking
    private var stepCountWindow = 0
    private var lastPeakTime = 0L
    private var isWalkingPattern = false

    // Historical window for speed calculation
    private val velocityHistory = ArrayDeque<Float>(10)

    // Owner learned motion profile
    private var ownerLearnedLiftSpeed: Float = AppPreferences.getOwnerLiftSpeed(context)
    private var ownerLearnedWalkVariance: Float = AppPreferences.getOwnerWalkVariance(context)

    fun notifySystemArmed() {
        armingTimestampMs = System.currentTimeMillis()
    }

    fun notifyTouchDetected() {
        lastTouchTimestampMs = System.currentTimeMillis()
    }

    fun isInArmingGracePeriod(): Boolean {
        if (armingTimestampMs == 0L) return false
        return (System.currentTimeMillis() - armingTimestampMs) < ARMING_GRACE_PERIOD_MS
    }

    fun isInTouchGracePeriod(): Boolean {
        if (lastTouchTimestampMs == 0L) return false
        return (System.currentTimeMillis() - lastTouchTimestampMs) < TOUCH_GRACE_PERIOD_MS
    }

    fun isGracePeriodActive(): Boolean {
        return isInArmingGracePeriod() || isInTouchGracePeriod()
    }

    /**
     * Updates and analyzes movement sample.
     */
    fun analyze(
        x: Float,
        y: Float,
        z: Float,
        totalAccel: Float,
        proximityNear: Boolean,
        wasProximityNear: Boolean,
        lightLux: Float,
        wasLightLux: Float,
        deviceContext: DeviceContext
    ): MotionAnalysisResult {
        val now = System.currentTimeMillis()
        val dtSec = if (lastTimestampMs > 0L) ((now - lastTimestampMs) / 1000f).coerceIn(0.01f, 0.5f) else 0.05f

        val deltaTotal = abs(totalAccel - lastTotalAccel)
        val deltaGravity = abs(totalAccel - 9.8f)

        // Inclination angle relative to flat surface
        val normZ = (z / totalAccel.coerceAtLeast(0.1f)).coerceIn(-1f, 1f)
        val inclinationDeg = Math.toDegrees(Math.acos(normZ.toDouble())).toFloat()
        val tiltAngleDelta = abs(inclinationDeg - lastInclinationDeg)

        // Estimated linear velocity delta (approx integral of acceleration delta)
        val instantaneousSpeed = deltaGravity * dtSec
        if (velocityHistory.size >= 10) {
            velocityHistory.removeFirst()
        }
        velocityHistory.addLast(instantaneousSpeed)
        val estimatedSpeed = velocityHistory.sum()

        // Upward lift along Y and Z axes
        val liftYDelta = y - lastAccelY
        val liftZDelta = z - lastAccelZ
        val isUpwardLiftVector = (liftYDelta > 1.2f || abs(liftZDelta) > 1.5f)

        // 1. Walking / Running Frequency Recognition:
        // Periodic oscillations around 1.5 Hz to 2.5 Hz with moderate amplitude
        if (deltaTotal in 1.2f..4.0f) {
            if (now - lastPeakTime in 350L..750L) {
                stepCountWindow++
                if (stepCountWindow >= 3) {
                    isWalkingPattern = true
                }
            } else if (now - lastPeakTime > 1200L) {
                stepCountWindow = 0
                isWalkingPattern = false
            }
            lastPeakTime = now
        } else if (deltaTotal < 0.8f && (now - lastPeakTime > 1500L)) {
            isWalkingPattern = false
            stepCountWindow = 0
        }

        // Check grace periods
        val inGrace = isGracePeriodActive()

        // 2. Classify Activity
        val activity = when {
            // Snatch: violent jerk > 5.5 m/s²
            deltaTotal >= SNATCH_JERK_THRESHOLD -> MovementActivity.VIOLENT_SNATCH

            // In-hand normal use
            deviceContext == DeviceContext.HAND || isInTouchGracePeriod() -> MovementActivity.IN_HAND_USE

            // Walking pattern detected or walking bumps
            isWalkingPattern || (deviceContext == DeviceContext.POCKET && deltaTotal in 0.8f..3.8f) -> MovementActivity.WALKING

            // Big deliberate lift: G-Force > 2.0, Tilt > 20°, Speed > 0.5 m/s
            deltaGravity >= MIN_GFORCE_LIFT_THRESHOLD && tiltAngleDelta >= MIN_TILT_LIFT_THRESHOLD_DEG && estimatedSpeed >= MIN_SPEED_THRESHOLD_MS && isUpwardLiftVector -> {
                MovementActivity.SUSPICIOUS_LIFT
            }

            // Stationary
            deltaGravity < 0.6f && tiltAngleDelta < 6.0f -> MovementActivity.STATIONARY

            else -> {
                if (deltaGravity > 1.5f && isUpwardLiftVector) MovementActivity.SUSPICIOUS_LIFT
                else MovementActivity.STATIONARY
            }
        }
        _currentActivity.value = activity

        // 3. Sensor Fusion Transition Check:
        // Proximity NEAR + Light DARK -> POCKET mode
        // Proximity FAR + Light BRIGHT -> HAND/TABLE mode
        // Only alarm on clear TRANSITION from POCKET to OUT (NEAR -> FAR + Light spike + Lift acceleration)
        val isPocketRemovalTransition = wasProximityNear && !proximityNear &&
                wasLightLux < 30f && lightLux > 40f &&
                (deltaGravity > 1.8f || estimatedSpeed > 0.4f)

        // 4. Evaluate Lift Trigger
        val isLiftTrigger = !inGrace &&
                deviceContext == DeviceContext.TABLE &&
                deltaGravity >= MIN_GFORCE_LIFT_THRESHOLD &&
                tiltAngleDelta >= MIN_TILT_LIFT_THRESHOLD_DEG &&
                estimatedSpeed >= MIN_SPEED_THRESHOLD_MS &&
                isUpwardLiftVector

        // 5. Evaluate Snatch Trigger
        val isSnatchTrigger = !inGrace && (deltaTotal >= SNATCH_JERK_THRESHOLD) && (deviceContext != DeviceContext.HAND)

        val explanation = buildString {
            if (inGrace) append("[Grace Period Active] ")
            append("Activity: ${activity.displayName} | G-Force: ${String.format("%.1f", deltaGravity)} m/s² | Tilt: ${String.format("%.1f", tiltAngleDelta)}° | Speed: ${String.format("%.2f", estimatedSpeed)} m/s")
        }

        // Update tracking states
        lastTotalAccel = totalAccel
        lastAccelX = x
        lastAccelY = y
        lastAccelZ = z
        lastInclinationDeg = inclinationDeg
        lastTimestampMs = now

        return MotionAnalysisResult(
            activity = activity,
            isLiftTrigger = isLiftTrigger,
            isSnatchTrigger = isSnatchTrigger,
            isPocketExtractionTrigger = isPocketRemovalTransition && !inGrace,
            isInGracePeriod = inGrace,
            gForceDelta = deltaGravity,
            tiltAngleDelta = tiltAngleDelta,
            estimatedSpeedMs = estimatedSpeed,
            explanation = explanation
        )
    }

    /**
     * Learn owner's lift pattern to avoid false alarms during owner handling.
     */
    fun recordOwnerLiftPattern(observedSpeed: Float) {
        val updated = (ownerLearnedLiftSpeed * 0.7f) + (observedSpeed * 0.3f)
        ownerLearnedLiftSpeed = updated
        AppPreferences.setOwnerLiftSpeed(context, updated)
        Log.i(TAG, "Learned owner lift speed: $updated m/s")
    }
}
