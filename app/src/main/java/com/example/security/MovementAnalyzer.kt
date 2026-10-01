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
    val rotationDeltaDeg: Float,
    val estimatedSpeedMs: Float,
    val isWalking: Boolean,
    val explanation: String
)

/**
 * MovementAnalyzer:
 * - Real-time activity classification (Stationary, In-Hand, Walking, Running, Lift, Snatch)
 * - Gait frequency and step cadence tracking
 * - Feeds incremental behavioral samples into BehaviorTrainer (Gait Pattern & Lift Pattern)
 */
class MovementAnalyzer(
    private val context: Context,
    var trainer: BehaviorTrainer? = null
) {
    companion object {
        private const val TAG = "MovementAnalyzer"

        // Threshold specifications:
        const val MIN_GFORCE_LIFT_THRESHOLD = 2.0f
        const val MIN_TILT_LIFT_THRESHOLD_DEG = 20.0f
        const val MIN_SPEED_THRESHOLD_MS = 0.5f
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
    var isWalkingPattern: Boolean = false
        private set

    // Historical window for speed calculation
    private val velocityHistory = ArrayDeque<Float>(10)

    // Owner learned motion profile
    private var ownerLearnedLiftSpeed: Float = AppPreferences.getOwnerLiftSpeed(context)

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

        // Estimated planar rotation delta
        val rotationDelta = abs(x - lastAccelX) + abs(y - lastAccelY)

        // Estimated linear velocity delta
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
        if (deltaTotal in 1.1f..4.2f) {
            val stepInterval = now - lastPeakTime
            if (stepInterval in 320L..800L) {
                stepCountWindow++
                if (stepCountWindow >= 3) {
                    isWalkingPattern = true
                    // Feed Day 7 Gait sample into BehaviorTrainer
                    val freqHz = (1000f / stepInterval).coerceIn(1.2f, 3.0f)
                    trainer?.recordGaitSample(
                        stepFrequencyHz = freqHz,
                        strideLengthMeters = 0.74f,
                        peakAcceleration = totalAccel,
                        walkingOscillationVariance = deltaTotal * 0.1f
                    )
                }
            } else if (stepInterval > 1200L) {
                stepCountWindow = 0
                isWalkingPattern = false
            }
            lastPeakTime = now
        } else if (deltaTotal < 0.7f && (now - lastPeakTime > 1500L)) {
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
            deviceContext == DeviceContext.HAND || isInTouchGracePeriod() -> {
                // If user is naturally lifting the phone in-hand, record as owner lift profile (Day 3-4)
                if (estimatedSpeed in 0.3f..2.0f && tiltAngleDelta > 10f) {
                    trainer?.recordLiftSample(
                        speedMs = estimatedSpeed,
                        angleDeg = tiltAngleDelta,
                        rotationDeg = rotationDelta * 10f,
                        timeToLiftMs = (dtSec * 1000f).coerceIn(300f, 1500f)
                    )
                }
                MovementActivity.IN_HAND_USE
            }

            // Walking pattern detected
            isWalkingPattern || (deviceContext == DeviceContext.POCKET && deltaTotal in 0.8f..3.8f) -> MovementActivity.WALKING

            // Suspicious lift from table: G-Force > 2.0, Tilt > 20°, Speed > 0.5 m/s
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

        // 3. Sensor Fusion Transition Check
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
            if (inGrace) append("[Grace Active] ")
            append("${activity.displayName} | Spd: ${String.format("%.2f", estimatedSpeed)}m/s | Tilt: ${String.format("%.1f", tiltAngleDelta)}° | G: ${String.format("%.1f", deltaGravity)}")
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
            rotationDeltaDeg = rotationDelta * 10f,
            estimatedSpeedMs = estimatedSpeed,
            isWalking = isWalkingPattern,
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
