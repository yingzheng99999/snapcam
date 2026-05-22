package com.snapcam.data.camera

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.snapcam.domain.model.CameraMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

sealed class CaptureResult {
    data class PhotoSaved(val file: File) : CaptureResult()
    data class VideoSaved(val uri: android.net.Uri, val durationMs: Long) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
}

class CameraManager(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var activeLens = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private var _isRecording = false
    val isRecording: Boolean get() = _isRecording
    private var currentVideoCallback: ((CaptureResult) -> Unit)? = null

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        mode: CameraMode = CameraMode.PHOTO,
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

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(
                    Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()
            videoCapture = VideoCapture.Builder(recorder).build()

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing).build()

            try {
                val useCases = mutableListOf<Any>(preview!!, imageCapture!!)
                if (mode == CameraMode.VIDEO) useCases.add(videoCapture!!)
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, selector, *useCases.toTypedArray()
                )
                activeLens = lensFacing
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto(onResult: (CaptureResult) -> Unit) {
        val cap = imageCapture ?: run { onResult(CaptureResult.Error("Not ready")); return }
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${dateFormat.format(System.currentTimeMillis())}.jpg")
        cap.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                    onResult(CaptureResult.PhotoSaved(file))
                }
                override fun onError(e: ImageCaptureException) {
                    onResult(CaptureResult.Error(e.message ?: "Capture failed", e))
                }
            }
        )
    }

    fun startVideoRecording(onResult: (CaptureResult) -> Unit) {
        val capture = videoCapture ?: run { onResult(CaptureResult.Error("Video not ready")); return }
        val timestamp = dateFormat.format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "VID_$timestamp.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: run { onResult(CaptureResult.Error("Failed to create video file")); return }

        val fdOptions = FileDescriptorOutputOptions.Builder(
            context.contentResolver.openFileDescriptor(uri, "w")!!
        ).build()

        currentVideoCallback = onResult
        _isRecording = true

        capture.startRecording(fdOptions, cameraExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> { /* recording started */ }
                is VideoRecordEvent.Finalize -> {
                    _isRecording = false
                    if (event.hasError()) {
                        currentVideoCallback?.invoke(
                            CaptureResult.Error("Recording failed: ${event.cause}")
                        )
                    } else {
                        currentVideoCallback?.invoke(
                            CaptureResult.VideoSaved(uri, event.recordingDuration)
                        )
                    }
                    currentVideoCallback = null
                }
                else -> {}
            }
        }
    }

    fun stopVideoRecording() {
        videoCapture?.stopRecording()
        _isRecording = false
    }

    fun switchLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView, mode: CameraMode) {
        val newLens = if (activeLens == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera(lifecycleOwner, previewView, mode, newLens)
    }

    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
