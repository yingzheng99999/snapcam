package com.snapcam.data.camera

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.snapcam.domain.model.CameraMode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class CaptureResult {
    data class PhotoSaved(val file: File) : CaptureResult()
    data class VideoSaved(val file: File) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
}

sealed class VideoState {
    data object Idle : VideoState()
    data object Recording : VideoState()
    data object Stopped : VideoState()
}

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var activeLens = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    var videoState: VideoState = VideoState.Idle
        private set

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        mode: CameraMode = CameraMode.PHOTO,
        lensFacing: Int = activeLens
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll()

            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy(
                    AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    AspectRatioStrategy.FALLBACK_RULE_NONE
                ))
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE)
                .build()

            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.FHD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                ).build()

            videoCapture = VideoCapture.Builder(recorder).build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val useCases = useCaseGroupForMode(mode)
            try {
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner, cameraSelector, *useCases.toTypedArray()
                )
                activeLens = lensFacing
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun useCaseGroupForMode(mode: CameraMode): List<Any> {
        val list = mutableListOf<Any>(preview!!)
        list.add(imageCapture!!)
        if (mode == CameraMode.VIDEO) {
            list.add(videoCapture!!)
        }
        return list
    }

    fun capturePhoto(onResult: (CaptureResult) -> Unit) {
        val capture = imageCapture ?: run {
            onResult(CaptureResult.Error("Camera not initialized"))
            return
        }
        val photoFile = createFile("IMG_", ".jpg", Environment.DIRECTORY_PICTURES)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onResult(CaptureResult.PhotoSaved(photoFile))
                }
                override fun onError(e: ImageCaptureException) {
                    onResult(CaptureResult.Error("Capture failed: ${e.message}", e))
                }
            }
        )
    }

    fun startVideoRecording(onEvent: (VideoRecordEvent) -> Unit) {
        val capture = videoCapture ?: run { return }
        val videoFile = createFile("VID_", ".mp4", Environment.DIRECTORY_MOVIES)
        val outputOptions = FileDescriptorOutputOptions.Builder(
            context.contentResolver.openFileDescriptor(
                createVideoContentUri(videoFile), "w"
            )!!).build()

        capture.startRecording(outputOptions, cameraExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> { videoState = VideoState.Recording }
                is VideoRecordEvent.Finalize -> { videoState = VideoState.Idle }
                else -> {}
            }
            onEvent(event)
        }
    }

    fun stopVideoRecording() {
        videoCapture?.stopRecording()
        videoState = VideoState.Stopped
    }

    fun switchLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView, mode: CameraMode) {
        val newLens = if (activeLens == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera(lifecycleOwner, previewView, mode, newLens)
    }

    private fun createFile(prefix: String, suffix: String, dir: String): File {
        val timestamp = fileDateFormat.format(System.currentTimeMillis())
        val directory = context.getExternalFilesDir(dir)
        return File(directory, "${prefix}${timestamp}$suffix")
    }

    private fun createVideoContentUri(file: File): android.net.Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
        return context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
        ) ?: android.net.Uri.fromFile(file)
    }

    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
