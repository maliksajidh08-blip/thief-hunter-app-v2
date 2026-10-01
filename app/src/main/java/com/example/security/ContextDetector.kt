package com.example.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * 5 Physical Device Contexts:
 * 1. HAND (Owner holding)
 * 2. LEG (On body)
 * 3. POCKET (In pocket)
 * 4. TABLE (On table)
 * 5. BAG (In bag)
 */
enum class DeviceContext(val displayName: String, val allowsAlarm: Boolean) {
    HAND("Owner Holding (Hand)", false),
    LEG("Resting on Leg / Body", false),
    POCKET("Inside Pocket", true),
    TABLE("Stationary on Table", true),
    BAG("Inside Bag", true);

    companion object {
        val LEG_OR_BODY: DeviceContext get() = LEG
    }
}

/**
 * ContextDetector uses sensor fusion (proximity, ambient light, micro-acceleration variance,
 * gravity orientation, and touch interaction history) to classify between all 5 contexts.
 */
class ContextDetector(private val context: Context) {

    companion object {
        private const val TAG = "ContextDetector"
        private const val WINDOW_SIZE = 14
    }

    private val _currentContext = MutableStateFlow(DeviceContext.TABLE)
    val currentContext: StateFlow<DeviceContext> = _currentContext.asStateFlow()

    private val accelHistory = ArrayDeque<Float>(WINDOW_SIZE)
    private var lastProximityNear: Boolean = false
    private var lastLightLux: Float = 200f
    private var lastContextUpdateTime: Long = 0L

    // Tracks when phone was touched or user began interacting
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
        totalAcceleration: Float,
        isWalkingMotion: Boolean = false
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

        // Orientation analysis (tilt angle relative to flat surface)
        val normZ = (accelZ / totalAcceleration.coerceAtLeast(0.1f)).coerceIn(-1f, 1f)
        val tiltAngleDeg = Math.toDegrees(Math.acos(normZ.toDouble())).toFloat()

        val recentlyInteracted = isUserRecentlyInteracting()

        // 1. HAND (Owner holding):
        // Proximity: Near or Far, Light: Bright (>20 lux), Motion: Active (variance in 0.03..1.5), Touch: Detected
        val isHand = recentlyInteracted || (!isProximityNear && lightLux > 20f && variance in 0.03f..1.5f && tiltAngleDeg in 15f..80f)

        // 2. LEG (On body):
        // Proximity: Near, Light: Medium (10..150 lux), Motion: Gentle (breathing variance 0.01..0.3), Touch: None
        val isLeg = !recentlyInteracted && isProximityNear && lightLux in 10f..180f && variance in 0.008f..0.35f && tiltAngleDeg in 20f..70f

        // 3. POCKET (In pocket):
        // Proximity: Near, Light: Dark (<30 lux), Motion: Walking pattern, Touch: None
        val isPocket = !recentlyInteracted && isProximityNear && lightLux < 30f && (isWalkingMotion || variance in 0.2f..3.5f)

        // 4. BAG (In bag):
        // Proximity: Near, Light: Dark (<30 lux), Motion: Random / irregular swaying (not rhythmic walking), Touch: None
        val isBag = !recentlyInteracted && isProximityNear && lightLux < 30f && !isWalkingMotion && (variance in 0.03f..0.8f)

        // 5. TABLE (On table):
        // Proximity: Far, Light: Bright (>25 lux), Motion: None (variance < 0.02, deltaFromGravity < 0.35), Touch: None
        val isTable = !recentlyInteracted && !isProximityNear && variance < 0.025f && deltaFromGravity < 0.4f && (tiltAngleDeg < 18f || tiltAngleDeg > 162f)

        val detectedContext = when {
            isHand -> DeviceContext.HAND
            isLeg -> DeviceContext.LEG
            isTable -> DeviceContext.TABLE
            isPocket -> DeviceContext.POCKET
            isBag -> DeviceContext.BAG
            // Fallbacks based on optical & proximity cues
            isProximityNear && lightLux < 30f -> if (isWalkingMotion) DeviceContext.POCKET else DeviceContext.BAG
            !isProximityNear && lightLux > 20f && variance > 0.03f -> DeviceContext.HAND
            !isProximityNear && variance < 0.03f -> DeviceContext.TABLE
            else -> _currentContext.value
        }

        _currentContext.value = detectedContext
        lastContextUpdateTime = now
        return detectedContext
    }

    /**
     * Determines whether an alarm is allowed based on the detected context.
     * HAND -> NO ALARM
     * LEG -> NO ALARM
     * TABLE -> Allowed for unauthorized lift
     * POCKET -> Allowed for pocket extraction / snatch
     * BAG -> Allowed for bag snatch
     */
    fun allowsAlarmTrigger(): Boolean {
        if (isUserRecentlyInteracting()) return false
        val ctx = _currentContext.value
        return ctx == DeviceContext.TABLE || ctx == DeviceContext.POCKET || ctx == DeviceContext.BAG
    }
}
