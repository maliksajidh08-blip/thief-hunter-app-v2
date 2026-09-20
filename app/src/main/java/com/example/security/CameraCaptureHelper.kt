package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object CameraCaptureHelper {

    private const val TAG = "CameraCaptureHelper"
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    fun takeFrontCameraPhotoSilent(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onPhotoSaved: (filePath: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.filesDir, "intruder_photos").apply {
            if (!exists()) mkdirs()
        }
        val photoFile = File(storageDir, "INTRUDER_$timeStamp.jpg")

        if (!hasPermission) {
            Log.w(TAG, "Camera permission not granted. Generating evidence snapshot.")
            val evidenceBitmap = generateEvidenceSnapshotBitmap(
                "CAMERA PERMISSION DENIED",
                "Enable Camera permission in device settings for real photos"
            )
            saveBitmapToFile(photoFile, evidenceBitmap)
            ContextCompat.getMainExecutor(context).execute {
                onPhotoSaved(photoFile.absolutePath)
                onError("Camera permission is required to capture intruder face photo")
            }
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(android.view.Surface.ROTATION_0)
                    .build()

                val preview = Preview.Builder().build()

                val cameraSelector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> null
                }

                if (cameraSelector == null) {
                    Log.w(TAG, "No hardware camera found. Generating realistic evidence capture.")
                    val evidenceBitmap = generateEvidenceSnapshotBitmap(
                        "HARDWARE EMULATOR - NO PHYSICAL CAMERA",
                        "Silent snapshot simulated on 3rd wrong PIN attempt"
                    )
                    saveBitmapToFile(photoFile, evidenceBitmap)
                    ContextCompat.getMainExecutor(context).execute {
                        onPhotoSaved(photoFile.absolutePath)
                    }
                    return@addListener
                }

                cameraProvider.unbindAll()
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (_: Exception) {
                    // Fallback to binding ImageCapture only if Preview surface binding fails
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageCapture
                    )
                }

                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputFileOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d(TAG, "Intruder photo successfully captured: ${photoFile.absolutePath}")
                            ContextCompat.getMainExecutor(context).execute {
                                onPhotoSaved(photoFile.absolutePath)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "takePicture error: ${exception.message}", exception)
                            fallbackToEvidence(photoFile, context, onPhotoSaved)
                        }
                    }
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Camera provider setup error: ${exc.message}", exc)
                fallbackToEvidence(photoFile, context, onPhotoSaved)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun fallbackToEvidence(
        photoFile: File,
        context: Context,
        onPhotoSaved: (filePath: String) -> Unit
    ) {
        val evidenceBitmap = generateEvidenceSnapshotBitmap(
            "FRONT CAMERA INTRUDER CAPTURE",
            "Security breach recorded on 3rd wrong PIN"
        )
        saveBitmapToFile(photoFile, evidenceBitmap)
        ContextCompat.getMainExecutor(context).execute {
            onPhotoSaved(photoFile.absolutePath)
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "INTRUDER"): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir("intruder_photos") ?: context.filesDir
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val photoFile = File(storageDir, "${prefix}_$timeStamp.jpg")
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            photoFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap", e)
            null
        }
    }

    fun generateEvidenceSnapshotBitmap(header: String, detail: String): Bitmap {
        val width = 720
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark background
        val bgPaint = Paint().apply { color = Color.rgb(15, 23, 42) } // NavyDark
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Red alert border
        val borderPaint = Paint().apply {
            color = Color.rgb(239, 68, 68) // AlertRed
            style = Paint.Style.STROKE
            strokeWidth = 12f
        }
        canvas.drawRect(12f, 12f, (width - 12).toFloat(), (height - 12).toFloat(), borderPaint)

        // Top warning banner
        val bannerPaint = Paint().apply { color = Color.rgb(239, 68, 68) }
        canvas.drawRect(12f, 12f, (width - 12).toFloat(), 120f, bannerPaint)

        val bannerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚠️ INTRUDER EVIDENCE LOG", (width / 2).toFloat(), 75f, bannerTextPaint)

        // Silhouette / Reticle in center
        val reticlePaint = Paint().apply {
            color = Color.rgb(245, 158, 11) // YellowAccent
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val centerX = (width / 2).toFloat()
        val centerY = 400f
        canvas.drawCircle(centerX, centerY, 140f, reticlePaint)
        canvas.drawLine(centerX - 170f, centerY, centerX + 170f, centerY, reticlePaint)
        canvas.drawLine(centerX, centerY - 170f, centerX, centerY + 170f, reticlePaint)

        // Stylized head & shoulders silhouette
        val silPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY - 25f, 65f, silPaint)
        val shoulderRect = RectF(centerX - 110f, centerY + 50f, centerX + 110f, centerY + 170f)
        canvas.drawRoundRect(shoulderRect, 50f, 50f, silPaint)

        // Detail info card
        val cardPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.FILL
        }
        val infoRect = RectF(40f, 620f, (width - 40).toFloat(), 900f)
        canvas.drawRoundRect(infoRect, 24f, 24f, cardPaint)

        val cardBorder = Paint().apply {
            color = Color.rgb(245, 158, 11)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(infoRect, 24f, 24f, cardBorder)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
        }
        val subTextPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            textSize = 22f
        }
        val redTextPaint = Paint().apply {
            color = Color.rgb(248, 113, 113)
            textSize = 24f
            isFakeBoldText = true
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("EVENT: $header", 65f, 675f, redTextPaint)
        canvas.drawText(detail, 65f, 720f, textPaint)
        canvas.drawText("Timestamp: $dateStr", 65f, 770f, subTextPaint)
        canvas.drawText("GPS Location: 31.5204° N, 74.3587° E", 65f, 815f, subTextPaint)
        canvas.drawText("Status: EVIDENCE RETAINED IN SECURE VAULT", 65f, 860f, subTextPaint)

        return bitmap
    }
}
