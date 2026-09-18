package com.example.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
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

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    fun takeFrontCameraPhotoSilent(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onPhotoSaved: (filePath: String) -> Unit,
        onError: (errorMessage: String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(android.view.Surface.ROTATION_0)
                    .build()

                // Check if front camera is available, fallback to back camera if not
                val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    onError("No camera available on device")
                    return@addListener
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture
                )

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val storageDir = context.getExternalFilesDir("intruder_photos") ?: context.filesDir
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }
                val photoFile = File(storageDir, "INTRUDER_$timeStamp.jpg")

                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            try {
                                val bitmap = imageProxyToBitmap(image)
                                image.close()
                                if (bitmap != null) {
                                    FileOutputStream(photoFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }
                                    ContextCompat.getMainExecutor(context).execute {
                                        onPhotoSaved(photoFile.absolutePath)
                                    }
                                } else {
                                    ContextCompat.getMainExecutor(context).execute {
                                        onError("Failed to decode camera image")
                                    }
                                }
                            } catch (e: Exception) {
                                image.close()
                                ContextCompat.getMainExecutor(context).execute {
                                    onError("Failed to process photo: ${e.message}")
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            ContextCompat.getMainExecutor(context).execute {
                                onError("Camera capture error: ${exception.message}")
                            }
                        }
                    }
                )
            } catch (exc: Exception) {
                onError("Camera initialization failed: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
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
            Log.e("CameraCaptureHelper", "Failed to save bitmap", e)
            null
        }
    }
}
