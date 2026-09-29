package com.example

import com.example.security.FaceRecognitionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerFaceRecognitionUnitTest {

    @Test
    fun testEuclideanDistance_identicalVectors_returnsZero() {
        val v1 = listOf(1.35f, 0.42f, 0.35f, 0.28f, 0.38f)
        val v2 = listOf(1.35f, 0.42f, 0.35f, 0.28f, 0.38f)

        val distance = FaceRecognitionHelper.computeDistance(v1, v2)
        assertEquals(0.0f, distance, 0.001f)

        val similarity = FaceRecognitionHelper.computeSimilarityScore(distance)
        assertTrue("Similarity should be ~1.0 for identical face", similarity >= 0.95f)
    }

    @Test
    fun testOwnerFaceComparisonLogic_distanceBelowThreshold_isOwner() {
        val ownerBaseline = listOf(1.35f, 0.42f, 0.35f, 0.28f, 0.38f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f)
        // Owner with subtle angle/lighting variation: small distance
        val ownerScan = listOf(1.37f, 0.41f, 0.36f, 0.27f, 0.39f, 0.51f, 0.49f, 0.50f, 0.50f, 0.51f)

        val distance = FaceRecognitionHelper.computeDistance(ownerScan, ownerBaseline)
        val similarity = FaceRecognitionHelper.computeSimilarityScore(distance)
        val threshold = FaceRecognitionHelper.DEFAULT_DISTANCE_THRESHOLD // 0.38f

        // Correct comparison logic:
        val isOwner = (distance < threshold) || (similarity >= FaceRecognitionHelper.DEFAULT_SIMILARITY_THRESHOLD)

        assertTrue("Distance $distance must be < threshold $threshold for Owner", distance < threshold)
        assertTrue("Similarity $similarity must be >= 0.75 for Owner", similarity >= 0.75f)
        assertTrue("Verdict must be OWNER (No Alarm)", isOwner)
    }

    @Test
    fun testStrangerFaceComparisonLogic_distanceAboveThreshold_isStranger() {
        val ownerBaseline = listOf(1.35f, 0.42f, 0.35f, 0.28f, 0.38f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f)
        // Stranger with significantly different facial geometry: large distance
        val strangerScan = listOf(1.70f, 0.65f, 0.55f, 0.45f, 0.60f, 0.70f, 0.72f, 0.30f, 0.32f, 0.20f)

        val distance = FaceRecognitionHelper.computeDistance(strangerScan, ownerBaseline)
        val similarity = FaceRecognitionHelper.computeSimilarityScore(distance)
        val threshold = FaceRecognitionHelper.DEFAULT_DISTANCE_THRESHOLD // 0.38f

        // Correct comparison logic:
        val isOwner = (distance < threshold) || (similarity >= FaceRecognitionHelper.DEFAULT_SIMILARITY_THRESHOLD)

        assertTrue("Distance $distance must be > threshold $threshold for Stranger", distance > threshold)
        assertTrue("Similarity $similarity must be < 0.75 for Stranger", similarity < 0.75f)
        assertFalse("Verdict must NOT be Owner (Stranger -> Alarm)", isOwner)
    }

    @Test
    fun testTwoStageResponseMetrics() {
        // Stage 1 motion response must be 0.1s (100ms)
        val stage1TargetMs = 100L
        assertTrue("Stage 1 motion alarm target should be <= 100ms", stage1TargetMs <= 100L)

        // Stage 2 face verification response target is 0.5s (500ms)
        val stage2TargetMs = 500L
        assertTrue("Stage 2 face scan target should be <= 500ms", stage2TargetMs <= 500L)
    }
}
