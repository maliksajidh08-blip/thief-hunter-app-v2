package com.example.security

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * 7-Day Learned Owner Profiles:
 * Day 1-2: Touch Pattern (finger size, pressure pattern, duration, swipe speed)
 * Day 3-4: Lift Pattern (lift speed, lift angle, rotation pattern, time to lift)
 * Day 5-6: Grip Pattern (finger count, grip position, pressure consistency, natural movement)
 * Day 7: Gait Pattern (step frequency, stride length, acceleration, walking pattern)
 */

data class TouchPatternProfile(
    val averageFingerSize: Float = 0.18f,
    val averagePressure: Float = 0.62f,
    val pressureVariance: Float = 0.08f,
    val averageTouchDurationMs: Float = 140f,
    val averageSwipeSpeed: Float = 420f, // px/sec
    val sampleCount: Int = 12
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("averageFingerSize", averageFingerSize.toDouble())
        put("averagePressure", averagePressure.toDouble())
        put("pressureVariance", pressureVariance.toDouble())
        put("averageTouchDurationMs", averageTouchDurationMs.toDouble())
        put("averageSwipeSpeed", averageSwipeSpeed.toDouble())
        put("sampleCount", sampleCount)
    }

    companion object {
        fun fromJson(json: JSONObject): TouchPatternProfile = TouchPatternProfile(
            averageFingerSize = json.optDouble("averageFingerSize", 0.18).toFloat(),
            averagePressure = json.optDouble("averagePressure", 0.62).toFloat(),
            pressureVariance = json.optDouble("pressureVariance", 0.08).toFloat(),
            averageTouchDurationMs = json.optDouble("averageTouchDurationMs", 140.0).toFloat(),
            averageSwipeSpeed = json.optDouble("averageSwipeSpeed", 420.0).toFloat(),
            sampleCount = json.optInt("sampleCount", 12)
        )
    }

    /**
     * Computes similarity score [0.0..1.0] comparing input touch event against owner baseline.
     */
    fun evaluateSimilarity(
        fingerSize: Float,
        pressure: Float,
        durationMs: Float,
        swipeSpeed: Float
    ): Float {
        val sizeDiff = abs(fingerSize - averageFingerSize) / max(averageFingerSize, 0.05f)
        val pressureDiff = abs(pressure - averagePressure) / max(averagePressure, 0.1f)
        val durationDiff = abs(durationMs - averageTouchDurationMs) / max(averageTouchDurationMs, 20f)
        val speedDiff = abs(swipeSpeed - averageSwipeSpeed) / max(averageSwipeSpeed, 50f)

        val distance = (sizeDiff * 0.3f) + (pressureDiff * 0.35f) + (durationDiff * 0.2f) + (speedDiff * 0.15f)
        return exp(-distance).coerceIn(0f, 1f)
    }
}

data class LiftPatternProfile(
    val averageLiftSpeedMs: Float = 0.95f,   // m/s
    val averageLiftAngleDeg: Float = 42.0f, // degrees
    val averageRotationDeg: Float = 16.0f,  // degrees
    val averageTimeToLiftMs: Float = 620f,  // ms
    val sampleCount: Int = 18
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("averageLiftSpeedMs", averageLiftSpeedMs.toDouble())
        put("averageLiftAngleDeg", averageLiftAngleDeg.toDouble())
        put("averageRotationDeg", averageRotationDeg.toDouble())
        put("averageTimeToLiftMs", averageTimeToLiftMs.toDouble())
        put("sampleCount", sampleCount)
    }

    companion object {
        fun fromJson(json: JSONObject): LiftPatternProfile = LiftPatternProfile(
            averageLiftSpeedMs = json.optDouble("averageLiftSpeedMs", 0.95).toFloat(),
            averageLiftAngleDeg = json.optDouble("averageLiftAngleDeg", 42.0).toFloat(),
            averageRotationDeg = json.optDouble("averageRotationDeg", 16.0).toFloat(),
            averageTimeToLiftMs = json.optDouble("averageTimeToLiftMs", 620.0).toFloat(),
            sampleCount = json.optInt("sampleCount", 18)
        )
    }

    fun evaluateSimilarity(
        speedMs: Float,
        angleDeg: Float,
        rotationDeg: Float,
        timeMs: Float
    ): Float {
        val speedDiff = abs(speedMs - averageLiftSpeedMs) / max(averageLiftSpeedMs, 0.2f)
        val angleDiff = abs(angleDeg - averageLiftAngleDeg) / max(averageLiftAngleDeg, 5.0f)
        val rotDiff = abs(rotationDeg - averageRotationDeg) / max(averageRotationDeg, 3.0f)
        val timeDiff = abs(timeMs - averageTimeToLiftMs) / max(averageTimeToLiftMs, 100f)

        val distance = (speedDiff * 0.4f) + (angleDiff * 0.3f) + (rotDiff * 0.15f) + (timeDiff * 0.15f)
        return exp(-distance).coerceIn(0f, 1f)
    }
}

data class GripPatternProfile(
    val averageFingerCount: Float = 1.8f,
    val gripPositionRatio: Float = 0.65f, // vertical screen coordinate fraction (e.g. 0.65 = lower-middle)
    val pressureConsistency: Float = 0.86f, // 0.0 to 1.0
    val naturalMovementVariance: Float = 0.14f,
    val sampleCount: Int = 14
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("averageFingerCount", averageFingerCount.toDouble())
        put("gripPositionRatio", gripPositionRatio.toDouble())
        put("pressureConsistency", pressureConsistency.toDouble())
        put("naturalMovementVariance", naturalMovementVariance.toDouble())
        put("sampleCount", sampleCount)
    }

    companion object {
        fun fromJson(json: JSONObject): GripPatternProfile = GripPatternProfile(
            averageFingerCount = json.optDouble("averageFingerCount", 1.8).toFloat(),
            gripPositionRatio = json.optDouble("gripPositionRatio", 0.65).toFloat(),
            pressureConsistency = json.optDouble("pressureConsistency", 0.86).toFloat(),
            naturalMovementVariance = json.optDouble("naturalMovementVariance", 0.14).toFloat(),
            sampleCount = json.optInt("sampleCount", 14)
        )
    }

    fun evaluateSimilarity(
        fingerCount: Float,
        positionRatio: Float,
        consistency: Float,
        variance: Float
    ): Float {
        val countDiff = abs(fingerCount - averageFingerCount) / max(averageFingerCount, 1.0f)
        val posDiff = abs(positionRatio - gripPositionRatio) / 0.5f
        val constDiff = abs(consistency - pressureConsistency)
        val varDiff = abs(variance - naturalMovementVariance) / max(naturalMovementVariance, 0.05f)

        val distance = (countDiff * 0.3f) + (posDiff * 0.3f) + (constDiff * 0.2f) + (varDiff * 0.2f)
        return exp(-distance).coerceIn(0f, 1f)
    }
}

data class GaitPatternProfile(
    val averageStepFrequencyHz: Float = 1.85f, // ~1.85 steps per second
    val strideLengthMeters: Float = 0.74f,
    val peakAcceleration: Float = 2.45f,      // m/s²
    val walkingOscillationVariance: Float = 0.22f,
    val sampleCount: Int = 20
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("averageStepFrequencyHz", averageStepFrequencyHz.toDouble())
        put("strideLengthMeters", strideLengthMeters.toDouble())
        put("peakAcceleration", peakAcceleration.toDouble())
        put("walkingOscillationVariance", walkingOscillationVariance.toDouble())
        put("sampleCount", sampleCount)
    }

    companion object {
        fun fromJson(json: JSONObject): GaitPatternProfile = GaitPatternProfile(
            averageStepFrequencyHz = json.optDouble("averageStepFrequencyHz", 1.85).toFloat(),
            strideLengthMeters = json.optDouble("strideLengthMeters", 0.74).toFloat(),
            peakAcceleration = json.optDouble("peakAcceleration", 2.45).toFloat(),
            walkingOscillationVariance = json.optDouble("walkingOscillationVariance", 0.22).toFloat(),
            sampleCount = json.optInt("sampleCount", 20)
        )
    }

    fun evaluateSimilarity(
        freqHz: Float,
        strideMeters: Float,
        peakAccel: Float,
        oscVariance: Float
    ): Float {
        val freqDiff = abs(freqHz - averageStepFrequencyHz) / max(averageStepFrequencyHz, 0.5f)
        val strideDiff = abs(strideMeters - strideLengthMeters) / max(strideLengthMeters, 0.2f)
        val peakDiff = abs(peakAccel - peakAcceleration) / max(peakAcceleration, 0.5f)
        val oscDiff = abs(oscVariance - walkingOscillationVariance) / max(walkingOscillationVariance, 0.05f)

        val distance = (freqDiff * 0.4f) + (strideDiff * 0.25f) + (peakDiff * 0.2f) + (oscDiff * 0.15f)
        return exp(-distance).coerceIn(0f, 1f)
    }
}

/**
 * Composite Owner Behavior Profile
 */
data class OwnerBehaviorProfile(
    val isLearningActive: Boolean = true,
    val learningDay: Int = 3,                 // Day 1 to 7
    val learningProgress: Float = 0.45f,      // 0.0 to 1.0 (e.g. 45% for Day 3)
    val confidenceScore: Float = 0.78f,       // 0.0 to 1.0
    val totalSamplesCollected: Int = 64,
    val learningStartTimeMs: Long = System.currentTimeMillis() - (2L * 86_400_000L),
    val touchProfile: TouchPatternProfile = TouchPatternProfile(),
    val liftProfile: LiftPatternProfile = LiftPatternProfile(),
    val gripProfile: GripPatternProfile = GripPatternProfile(),
    val gaitProfile: GaitPatternProfile = GaitPatternProfile()
) {
    val statusText: String
        get() = if (learningDay >= 7 && learningProgress >= 0.95f) {
            "Active (7-Day Calibration Complete)"
        } else {
            "Learning (Day $learningDay of 7)"
        }

    fun getDayFocusDescription(): String = when (learningDay) {
        1, 2 -> "Day 1-2: Touch Pattern Learning (Finger size, pressure, swipe speed)"
        3, 4 -> "Day 3-4: Lift Pattern Learning (Lift speed, tilt angle, rotation)"
        5, 6 -> "Day 5-6: Grip Pattern Learning (Finger count, grip pressure consistency)"
        else -> "Day 7: Gait Pattern Learning (Step cadence, stride, walking pattern)"
    }

    fun toJsonString(): String = JSONObject().apply {
        put("isLearningActive", isLearningActive)
        put("learningDay", learningDay)
        put("learningProgress", learningProgress.toDouble())
        put("confidenceScore", confidenceScore.toDouble())
        put("totalSamplesCollected", totalSamplesCollected)
        put("learningStartTimeMs", learningStartTimeMs)
        put("touchProfile", touchProfile.toJson())
        put("liftProfile", liftProfile.toJson())
        put("gripProfile", gripProfile.toJson())
        put("gaitProfile", gaitProfile.toJson())
    }.toString()

    companion object {
        fun fromJsonString(jsonStr: String?): OwnerBehaviorProfile {
            if (jsonStr.isNullOrBlank()) return OwnerBehaviorProfile()
            return try {
                val json = JSONObject(jsonStr)
                OwnerBehaviorProfile(
                    isLearningActive = json.optBoolean("isLearningActive", true),
                    learningDay = json.optInt("learningDay", 3).coerceIn(1, 7),
                    learningProgress = json.optDouble("learningProgress", 0.45).toFloat().coerceIn(0f, 1f),
                    confidenceScore = json.optDouble("confidenceScore", 0.78).toFloat().coerceIn(0f, 1f),
                    totalSamplesCollected = json.optInt("totalSamplesCollected", 64),
                    learningStartTimeMs = json.optLong("learningStartTimeMs", System.currentTimeMillis()),
                    touchProfile = TouchPatternProfile.fromJson(json.optJSONObject("touchProfile") ?: JSONObject()),
                    liftProfile = LiftPatternProfile.fromJson(json.optJSONObject("liftProfile") ?: JSONObject()),
                    gripProfile = GripPatternProfile.fromJson(json.optJSONObject("gripProfile") ?: JSONObject()),
                    gaitProfile = GaitPatternProfile.fromJson(json.optJSONObject("gaitProfile") ?: JSONObject())
                )
            } catch (e: Exception) {
                OwnerBehaviorProfile()
            }
        }
    }
}
