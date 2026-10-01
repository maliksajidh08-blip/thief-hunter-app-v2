package com.example.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * BehaviorTrainer manages the 7-Day Learning Phase:
 * - Day 1-2: Touch Pattern Learning
 * - Day 3-4: Lift Pattern Learning
 * - Day 5-6: Grip Pattern Learning
 * - Day 7: Gait Pattern Learning
 *
 * Persists real-time incremental samples to storage and calculates confidence progress.
 */
class BehaviorTrainer(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private const val TAG = "BehaviorTrainer"
        private const val PREFS_NAME = "thief_hunter_ai_profile_prefs"
        private const val KEY_PROFILE_JSON = "key_owner_profile_json"
        const val TOTAL_DAYS = 7
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadInitialProfile())
    val profile: StateFlow<OwnerBehaviorProfile> = _profile.asStateFlow()

    init {
        // Automatically check if day should advance based on real calendar time
        checkAutoAdvanceDay()
    }

    private fun loadInitialProfile(): OwnerBehaviorProfile {
        val saved = prefs.getString(KEY_PROFILE_JSON, null)
        return if (!saved.isNullOrBlank()) {
            OwnerBehaviorProfile.fromJsonString(saved)
        } else {
            // Default 3-day active demo state as requested in UI specification
            val initial = OwnerBehaviorProfile(
                isLearningActive = true,
                learningDay = 3,
                learningProgress = 0.45f,
                confidenceScore = 0.45f,
                totalSamplesCollected = 45
            )
            saveProfile(initial)
            initial
        }
    }

    private fun saveProfile(p: OwnerBehaviorProfile) {
        prefs.edit().putString(KEY_PROFILE_JSON, p.toJsonString()).apply()
    }

    private fun checkAutoAdvanceDay() {
        val current = _profile.value
        if (!current.isLearningActive) return

        val now = System.currentTimeMillis()
        val elapsedDays = ((now - current.learningStartTimeMs) / 86_400_000L).toInt() + 1
        val targetDay = elapsedDays.coerceIn(1, 7)

        if (targetDay > current.learningDay) {
            val newProgress = min(1.0f, (targetDay.toFloat() / TOTAL_DAYS.toFloat()))
            val newConfidence = min(0.96f, 0.25f + (newProgress * 0.70f))
            val updated = current.copy(
                learningDay = targetDay,
                learningProgress = newProgress,
                confidenceScore = newConfidence
            )
            _profile.value = updated
            saveProfile(updated)
            Log.i(TAG, "Advanced AI learning to Day $targetDay (Progress: ${(newProgress * 100).toInt()}%)")
        }
    }

    fun startLearning() {
        val current = _profile.value
        val updated = current.copy(
            isLearningActive = true,
            learningStartTimeMs = System.currentTimeMillis() - ((current.learningDay - 1) * 86_400_000L)
        )
        _profile.value = updated
        saveProfile(updated)
        Log.i(TAG, "Started AI Learning: Day ${updated.learningDay}")
    }

    fun resetLearning() {
        val fresh = OwnerBehaviorProfile(
            isLearningActive = true,
            learningDay = 1,
            learningProgress = 0.14f,
            confidenceScore = 0.20f,
            totalSamplesCollected = 0,
            learningStartTimeMs = System.currentTimeMillis(),
            touchProfile = TouchPatternProfile(sampleCount = 0),
            liftProfile = LiftPatternProfile(sampleCount = 0),
            gripProfile = GripPatternProfile(sampleCount = 0),
            gaitProfile = GaitPatternProfile(sampleCount = 0)
        )
        _profile.value = fresh
        saveProfile(fresh)
        Log.i(TAG, "Reset AI Learning System to Day 1")
    }

    fun advanceDayManually() {
        val current = _profile.value
        val nextDay = (current.learningDay % 7) + 1
        val newProgress = min(1.0f, nextDay.toFloat() / 7f)
        val newConfidence = min(0.96f, 0.25f + (newProgress * 0.70f))
        val updated = current.copy(
            learningDay = nextDay,
            learningProgress = newProgress,
            confidenceScore = newConfidence
        )
        _profile.value = updated
        saveProfile(updated)
    }

    // ==========================================
    // Day 1-2: Touch Pattern Learning
    // ==========================================
    fun recordTouchSample(
        fingerSize: Float,
        pressure: Float,
        durationMs: Float,
        swipeSpeed: Float
    ) {
        val current = _profile.value
        if (!current.isLearningActive) return

        val prev = current.touchProfile
        val n = prev.sampleCount
        val alpha = if (n > 0) 1.0f / (n + 1).coerceAtMost(30) else 1.0f

        val newSize = (prev.averageFingerSize * (1f - alpha)) + (fingerSize * alpha)
        val newPressure = (prev.averagePressure * (1f - alpha)) + (pressure * alpha)
        val newVariance = (prev.pressureVariance * (1f - alpha)) + (kotlin.math.abs(pressure - newPressure) * alpha)
        val newDuration = (prev.averageTouchDurationMs * (1f - alpha)) + (durationMs * alpha)
        val newSwipe = (prev.averageSwipeSpeed * (1f - alpha)) + (swipeSpeed * alpha)

        val updatedTouch = prev.copy(
            averageFingerSize = newSize,
            averagePressure = newPressure,
            pressureVariance = newVariance,
            averageTouchDurationMs = newDuration,
            averageSwipeSpeed = newSwipe,
            sampleCount = n + 1
        )

        val totalSamples = current.totalSamplesCollected + 1
        val boost = (totalSamples * 0.002f).coerceAtMost(0.15f)
        val newConfidence = (current.confidenceScore + boost).coerceAtMost(0.98f)

        val updatedProfile = current.copy(
            touchProfile = updatedTouch,
            totalSamplesCollected = totalSamples,
            confidenceScore = newConfidence
        )
        _profile.value = updatedProfile
        saveProfile(updatedProfile)
    }

    // ==========================================
    // Day 3-4: Lift Pattern Learning
    // ==========================================
    fun recordLiftSample(
        speedMs: Float,
        angleDeg: Float,
        rotationDeg: Float,
        timeToLiftMs: Float
    ) {
        val current = _profile.value
        if (!current.isLearningActive) return

        val prev = current.liftProfile
        val n = prev.sampleCount
        val alpha = if (n > 0) 1.0f / (n + 1).coerceAtMost(30) else 1.0f

        val newSpeed = (prev.averageLiftSpeedMs * (1f - alpha)) + (speedMs * alpha)
        val newAngle = (prev.averageLiftAngleDeg * (1f - alpha)) + (angleDeg * alpha)
        val newRotation = (prev.averageRotationDeg * (1f - alpha)) + (rotationDeg * alpha)
        val newTime = (prev.averageTimeToLiftMs * (1f - alpha)) + (timeToLiftMs * alpha)

        val updatedLift = prev.copy(
            averageLiftSpeedMs = newSpeed,
            averageLiftAngleDeg = newAngle,
            averageRotationDeg = newRotation,
            averageTimeToLiftMs = newTime,
            sampleCount = n + 1
        )

        val updatedProfile = current.copy(
            liftProfile = updatedLift,
            totalSamplesCollected = current.totalSamplesCollected + 1
        )
        _profile.value = updatedProfile
        saveProfile(updatedProfile)
    }

    // ==========================================
    // Day 5-6: Grip Pattern Learning
    // ==========================================
    fun recordGripSample(
        fingerCount: Float,
        gripPositionRatio: Float,
        pressureConsistency: Float,
        naturalMovementVariance: Float
    ) {
        val current = _profile.value
        if (!current.isLearningActive) return

        val prev = current.gripProfile
        val n = prev.sampleCount
        val alpha = if (n > 0) 1.0f / (n + 1).coerceAtMost(30) else 1.0f

        val newCount = (prev.averageFingerCount * (1f - alpha)) + (fingerCount * alpha)
        val newPos = (prev.gripPositionRatio * (1f - alpha)) + (gripPositionRatio * alpha)
        val newConsistency = (prev.pressureConsistency * (1f - alpha)) + (pressureConsistency * alpha)
        val newVariance = (prev.naturalMovementVariance * (1f - alpha)) + (naturalMovementVariance * alpha)

        val updatedGrip = prev.copy(
            averageFingerCount = newCount,
            gripPositionRatio = newPos,
            pressureConsistency = newConsistency,
            naturalMovementVariance = newVariance,
            sampleCount = n + 1
        )

        val updatedProfile = current.copy(
            gripProfile = updatedGrip,
            totalSamplesCollected = current.totalSamplesCollected + 1
        )
        _profile.value = updatedProfile
        saveProfile(updatedProfile)
    }

    // ==========================================
    // Day 7: Gait Pattern Learning
    // ==========================================
    fun recordGaitSample(
        stepFrequencyHz: Float,
        strideLengthMeters: Float,
        peakAcceleration: Float,
        walkingOscillationVariance: Float
    ) {
        val current = _profile.value
        if (!current.isLearningActive) return

        val prev = current.gaitProfile
        val n = prev.sampleCount
        val alpha = if (n > 0) 1.0f / (n + 1).coerceAtMost(30) else 1.0f

        val newFreq = (prev.averageStepFrequencyHz * (1f - alpha)) + (stepFrequencyHz * alpha)
        val newStride = (prev.strideLengthMeters * (1f - alpha)) + (strideLengthMeters * alpha)
        val newPeak = (prev.peakAcceleration * (1f - alpha)) + (peakAcceleration * alpha)
        val newOsc = (prev.walkingOscillationVariance * (1f - alpha)) + (walkingOscillationVariance * alpha)

        val updatedGait = prev.copy(
            averageStepFrequencyHz = newFreq,
            strideLengthMeters = newStride,
            peakAcceleration = newPeak,
            walkingOscillationVariance = newOsc,
            sampleCount = n + 1
        )

        val updatedProfile = current.copy(
            gaitProfile = updatedGait,
            totalSamplesCollected = current.totalSamplesCollected + 1
        )
        _profile.value = updatedProfile
        saveProfile(updatedProfile)
    }
}
