package com.example.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Physical context of the device to prevent false alarms.
 */
enum class DeviceContext(val displayName: String, val allowsAlarm: Boolean) {
    HAND("User Holding in Hand", false),
    POCKET("Inside Pocket / Bag", true),
    TABLE("Stationary on Table / Desk", true),
    LEG_OR_BODY("Resting on Leg / Body", false)
}

/**
 * ContextDetector combines multiple sensor cues (proximity, ambient light, gravity orientation,
 * and micro-tremor variance) to classify whether the user is actively using the phone,
 * carrying it on their leg/body, resting it on a table, or has it stowed in a pocket.
 */
class ContextDetector(private val context: Context) {

    companion object {
        private const val TAG = "ContextDetector"
        private const val WINDOW_SIZE = 12
    }

    private val _currentContext = MutableStateFlow(DeviceContext.TABLE)
    val currentContext: StateFlow<DeviceContext> = _currentContext.asStateFlow()

    private val accelHistory = ArrayDeque<Float>(WINDOW_SIZE)
    private var lastProximityNear: Boolean = false
    private var lastLightLux: Float = 200f
    private var lastContextUpdateTime: Long = 0L

    // Tracks when phone was touched or user began holding it to establish grace periods
    private var lastUserInteractionTime: Long = 0L

    fun notifyUserTouchInteraction() {
        lastUserInteractionTime = System.currentTimeMillis()
        _currentContext.value = DeviceContext.HAND
    }

    fun isUserRecentlyInteracting(gracePeriodMs: Long = 3000L): Boolean {
        return (System.currentTimeMillis() - lastUserInteractionTime) < gracePeriodMs
    }

    /**
     * Updates context from real-time sensor measurements.
     */
    fun update(
        isProximityNear: Boolean,
        lightLux: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        totalAcceleration: Float
    ): DeviceContext {
        val now = System.currentTimeMillis()
        lastProximityNear = isProximityNear
        lastLightLux = lightLux

        if (accelHistory.size >= WINDOW_SIZE) {
            accelHistory.removeFirst()
        }
        accelHistory.addLast(totalAcceleration)

        // Compute statistical variance of acceleration
        val avg = accelHistory.average().toFloat()
        var variance = 0f
        for (v in accelHistory) {
            variance += (v - avg) * (v - avg)
        }
        variance /= accelHistory.size.coerceAtLeast(1)

        val deltaFromGravity = abs(totalAcceleration - 9.8f)

        // Orientation analysis
        val normZ = (accelZ / totalAcceleration.coerceAtLeast(0.1f)).coerceIn(-1f, 1f)
        val tiltAngleDeg = Math.toDegrees(Math.acos(normZ.toDouble())).toFloat()

        // 1. POCKET DETECTION:
        // Proximity NEAR (< 3cm) AND Ambient Light DARK (< 30 lux)
        val isPocketEnvironment = isProximityNear && lightLux < 30f

        val detectedContext = when {
            isPocketEnvironment -> {
                DeviceContext.POCKET
            }

            // If recently touched by user (within 3 seconds):
            isUserRecentlyInteracting() -> {
                DeviceContext.HAND
            }

            // 2. HAND DETECTION:
            // Proximity is FAR, light is visible (> 25 lux).
            // Human hand holding exhibits characteristic micro-tremors (variance between 0.04 and 1.5),
            // and typical handheld viewing inclination (20° to 75° tilt).
            !isProximityNear && lightLux > 20f && variance in 0.03f..1.2f && tiltAngleDeg in 18f..80f -> {
                DeviceContext.HAND
            }

            // 3. LEG / BODY DETECTION:
            // Proximity is far or partially obstructed, light is low/medium (e.g. lap/thigh),
            // subtle gentle breathing movement or sitting sway (low variance < 0.25),
            // tilt corresponds to lying on thigh or knee (30° - 60° tilt).
            // NOT an unauthorized snatch!
            lightLux < 100f && variance in 0.01f..0.35f && tiltAngleDeg in 25f..65f && deltaFromGravity < 1.0f -> {
                DeviceContext.LEG_OR_BODY
            }

            // 4. TABLE DETECTION:
            // Resting still on flat/hard surface.
            // Variance is practically zero (< 0.02), total accel is close to 9.8 m/s²,
            // flat orientation (tilt < 15° or nearly 180° face down).
            variance < 0.025f && deltaFromGravity < 0.45f && (tiltAngleDeg < 16f || tiltAngleDeg > 165f) -> {
                DeviceContext.TABLE
            }

            else -> {
                // Default based on proximity & light
                if (isProximityNear && lightLux < 45f) {
                    DeviceContext.POCKET
                } else if (!isProximityNear && variance > 0.04f) {
                    DeviceContext.HAND
                } else {
                    _currentContext.value
                }
            }
        }

        _currentContext.value = detectedContext
        lastContextUpdateTime = now
        return detectedContext
    }

    /**
     * Determines whether an alarm is allowed based on the detected context.
     * HAND -> NO ALARM
     * LEG_OR_BODY -> NO ALARM
     * TABLE -> Allowed only for confirmed lift
     * POCKET -> Allowed only for extraction transition or violent snatch
     */
    fun allowsAlarmTrigger(): Boolean {
        if (isUserRecentlyInteracting()) return false
        val ctx = _currentContext.value
        return ctx == DeviceContext.TABLE || ctx == DeviceContext.POCKET
    }
}
