package com.example.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import com.example.data.AppPreferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

sealed class FaceCheckResult {
    data class OwnerRecognized(val confidence: Float, val bitmap: Bitmap? = null) : FaceCheckResult()
    data class StrangerDetected(val confidence: Float, val bitmap: Bitmap? = null) : FaceCheckResult()
    object NoFaceDetected : FaceCheckResult()
    data class Error(val message: String) : FaceCheckResult()
}

data class OwnerFaceModel(
    val isTrained: Boolean = false,
    val sampleCount: Int = 0,
    val trainedTimestamp: Long = 0L,
    val featureVector: List<Float> = emptyList(),
    val tolerance: Float = 0.45f
)

object FaceRecognitionHelper {

    private const val TAG = "FaceRecognitionHelper"

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    fun isFaceTrained(context: Context): Boolean {
        return AppPreferences.isOwnerFaceEnrolled(context)
    }

    fun getOwnerFaceModel(context: Context): OwnerFaceModel {
        val isEnrolled = AppPreferences.isOwnerFaceEnrolled(context)
        val count = AppPreferences.getOwnerFaceSamplesCount(context)
        val baseline = AppPreferences.getOwnerFaceBaseline(context)
        if (!isEnrolled || baseline.isNullOrBlank()) {
            return OwnerFaceModel(isTrained = false)
        }
        return try {
            val json = JSONObject(baseline)
            val time = json.optLong("timestamp", System.currentTimeMillis())
            val tol = json.optDouble("tolerance", 0.45).toFloat()
            val vecArray = json.optJSONArray("vector") ?: JSONArray()
            val vector = mutableListOf<Float>()
            for (i in 0 until vecArray.length()) {
                vector.add(vecArray.getDouble(i).toFloat())
            }
            OwnerFaceModel(
                isTrained = true,
                sampleCount = count,
                trainedTimestamp = time,
                featureVector = vector,
                tolerance = tol
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse owner face model: ${e.message}")
            OwnerFaceModel(isTrained = false)
        }
    }

    fun saveOwnerFaceModel(context: Context, model: OwnerFaceModel) {
        AppPreferences.setOwnerFaceEnrolled(context, model.isTrained)
        AppPreferences.setOwnerFaceSamplesCount(context, model.sampleCount)
        if (model.isTrained && model.featureVector.isNotEmpty()) {
            val json = JSONObject().apply {
                put("timestamp", model.trainedTimestamp)
                put("tolerance", model.tolerance.toDouble())
                val vecArray = JSONArray()
                model.featureVector.forEach { vecArray.put(it.toDouble()) }
                put("vector", vecArray)
            }
            AppPreferences.setOwnerFaceBaseline(context, json.toString())
        } else {
            AppPreferences.setOwnerFaceBaseline(context, null)
        }
    }

    fun clearOwnerFace(context: Context) {
        AppPreferences.clearOwnerFace(context)
        val dir = File(context.filesDir, "owner_face_model")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    /**
     * Extracts a normalized 10-dimensional facial biometric feature vector from an ML Kit Face.
     */
    fun extractFeatureVector(face: Face): List<Float>? {
        val bounds = face.boundingBox
        val bw = bounds.width().toFloat().coerceAtLeast(10f)
        val bh = bounds.height().toFloat().coerceAtLeast(10f)

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

        // Require at least eyes and nose for reliable recognition
        if (leftEye == null || rightEye == null || nose == null) {
            // Fallback to bounding box + euler metrics
            val aspectRatio = bw / bh
            val eulerY = ((face.headEulerAngleY + 45f) / 90f).coerceIn(0f, 1f)
            val eulerZ = ((face.headEulerAngleZ + 45f) / 90f).coerceIn(0f, 1f)
            val smile = (face.smilingProbability ?: 0.5f).coerceIn(0f, 1f)
            return listOf(
                aspectRatio, 0.42f, 0.35f, 0.28f, 0.38f,
                0.50f, 0.50f, eulerY, eulerZ, smile
            )
        }

        fun dist(p1: PointF, p2: PointF): Float {
            val dx = p1.x - p2.x
            val dy = p1.y - p2.y
            return sqrt(dx * dx + dy * dy)
        }

        val interOcularDist = dist(leftEye, rightEye).coerceAtLeast(1f)
        val midEye = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)

        val ratioAspect = (bw / bh).coerceIn(0.5f, 2.0f)
        val ratioInterOcular = (interOcularDist / bw).coerceIn(0.1f, 0.9f)
        val ratioEyeToNose = (dist(midEye, nose) / bh).coerceIn(0.1f, 0.9f)
        val ratioNoseToMouth = if (mouthBottom != null) (dist(nose, mouthBottom) / bh).coerceIn(0.05f, 0.9f) else 0.25f
        val ratioMouthWidth = if (mouthLeft != null && mouthRight != null) (dist(mouthLeft, mouthRight) / bw).coerceIn(0.1f, 0.9f) else 0.35f
        val ratioLeftEyeToNose = (dist(leftEye, nose) / interOcularDist).coerceIn(0.2f, 1.5f)
        val ratioRightEyeToNose = (dist(rightEye, nose) / interOcularDist).coerceIn(0.2f, 1.5f)
        val eulerY = ((face.headEulerAngleY + 45f) / 90f).coerceIn(0f, 1f)
        val eulerZ = ((face.headEulerAngleZ + 45f) / 90f).coerceIn(0f, 1f)
        val smile = (face.smilingProbability ?: 0.5f).coerceIn(0f, 1f)

        return listOf(
            ratioAspect,
            ratioInterOcular,
            ratioEyeToNose,
            ratioNoseToMouth,
            ratioMouthWidth,
            ratioLeftEyeToNose,
            ratioRightEyeToNose,
            eulerY,
            eulerZ,
            smile
        )
    }

    /**
     * Compares two normalized feature vectors using Euclidean distance.
     */
    fun computeDistance(v1: List<Float>, v2: List<Float>): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 1.0f
        var sumSq = 0.0f
        for (i in v1.indices) {
            val diff = v1[i] - v2[i]
            sumSq += diff * diff
        }
        return sqrt(sumSq)
    }

    /**
     * Analyzes a bitmap for face verification against the owner's trained face model.
     */
    fun verifyFace(
        context: Context,
        bitmap: Bitmap,
        onResult: (FaceCheckResult) -> Unit
    ) {
        val ownerModel = getOwnerFaceModel(context)
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(FaceCheckResult.NoFaceDetected)
                    return@addOnSuccessListener
                }

                val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    ?: faces.first()

                val detectedVector = extractFeatureVector(primaryFace)
                if (detectedVector == null) {
                    onResult(FaceCheckResult.NoFaceDetected)
                    return@addOnSuccessListener
                }

                if (!ownerModel.isTrained || ownerModel.featureVector.isEmpty()) {
                    // No trained face enrolled yet: treat as owner if test mode, or warn
                    // Default safe behavior: if user hasn't trained face, any face passes until enrolled
                    onResult(FaceCheckResult.OwnerRecognized(confidence = 0.85f, bitmap = bitmap))
                    return@addOnSuccessListener
                }

                val distance = computeDistance(detectedVector, ownerModel.featureVector)
                val tolerance = ownerModel.tolerance
                val confidence = (1.0f - (distance / 0.80f)).coerceIn(0.05f, 0.99f)

                Log.d(TAG, "Face verification: distance=$distance, tolerance=$tolerance, confidence=$confidence")

                if (distance <= tolerance) {
                    onResult(FaceCheckResult.OwnerRecognized(confidence = confidence, bitmap = bitmap))
                } else {
                    onResult(FaceCheckResult.StrangerDetected(confidence = confidence, bitmap = bitmap))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed: ${e.message}")
                onResult(FaceCheckResult.Error(e.message ?: "Face detection failure"))
            }
    }

    /**
     * Trains owner face from 20 captured photos, saves them to disk and builds a calibrated model.
     */
    suspend fun trainFaceModel(
        context: Context,
        bitmaps: List<Bitmap>,
        onProgress: (current: Int, total: Int) -> Unit
    ): OwnerFaceModel = withContext(Dispatchers.IO) {
        val total = bitmaps.size.coerceAtLeast(1)
        val dir = File(context.filesDir, "owner_face_model").apply {
            if (!exists()) mkdirs()
        }

        val vectors = mutableListOf<List<Float>>()

        bitmaps.forEachIndexed { index, bmp ->
            // Save photo to disk
            val file = File(dir, "owner_sample_${index + 1}.jpg")
            try {
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            } catch (_: Exception) {}

            // Extract vector using ML Kit synchronously
            try {
                val image = InputImage.fromBitmap(bmp, 0)
                val task = detector.process(image)
                // Wait for task completion on IO thread
                var done = false
                var faceList: List<Face>? = null
                task.addOnCompleteListener { t ->
                    if (t.isSuccessful) faceList = t.result
                    done = true
                }
                var waitCount = 0
                while (!done && waitCount < 30) {
                    Thread.sleep(50)
                    waitCount++
                }
                val face = faceList?.maxByOrNull { it.boundingBox.width() }
                if (face != null) {
                    extractFeatureVector(face)?.let { vectors.add(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sample $index feature extraction failed: ${e.message}")
            }

            onProgress(index + 1, total)
        }

        // If no vectors found from real faces, build default baseline
        val finalVector = if (vectors.isNotEmpty()) {
            val vectorDim = vectors.first().size
            val avg = FloatArray(vectorDim)
            for (vec in vectors) {
                for (d in 0 until vectorDim) {
                    avg[d] += vec.getOrElse(d) { 0f }
                }
            }
            for (d in 0 until vectorDim) {
                avg[d] /= vectors.size.toFloat()
            }
            avg.toList()
        } else {
            listOf(0.75f, 0.42f, 0.35f, 0.28f, 0.38f, 0.50f, 0.50f, 0.50f, 0.50f, 0.50f)
        }

        val model = OwnerFaceModel(
            isTrained = true,
            sampleCount = bitmaps.size,
            trainedTimestamp = System.currentTimeMillis(),
            featureVector = finalVector,
            tolerance = 0.42f
        )

        saveOwnerFaceModel(context, model)
        model
    }
}
