package com.snapcam.data.camera

import android.content.Context
import android.os.Environment
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
sealed class CaptureResult {
    data class PhotoSaved(val file: File) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
}

class CameraManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var activeLens = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = activeLens
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            cameraProvider?.unbindAll()

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing).build()

            try {
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, selector, preview!!, imageCapture!!
                )
                activeLens = lensFacing
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto(onResult: (CaptureResult) -> Unit) {
        val capture = imageCapture ?: run { onResult(CaptureResult.Error("Not ready")); return }
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${dateFormat.format(System.currentTimeMillis())}.jpg")
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(o: ImageCapture.OutputFileResults) { onResult(CaptureResult.PhotoSaved(file)) }
                override fun onError(e: ImageCaptureException) { onResult(CaptureResult.Error(e.message ?: "Capture failed", e)) }
            }
        )
    }

    fun switchLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val newLens = if (activeLens == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera(lifecycleOwner, previewView, newLens)
    }

    fun release() {
        cameraProvider?.unbindAll(); cameraExecutor.shutdown()
    }
}
