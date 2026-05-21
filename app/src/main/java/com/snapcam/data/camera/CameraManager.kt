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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

sealed class CaptureResult {
    data class PhotoSaved(val file: File) : CaptureResult()
    data class VideoSaved(val file: File) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
}

enum class VideoState { IDLE, RECORDING, STOPPED }

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
    var videoState: VideoState = VideoState.IDLE
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

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
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

            val useCases = mutableListOf<Any>(preview!!, imageCapture!!)
            if (mode == CameraMode.VIDEO) useCases.add(videoCapture!!)

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
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: return
        val outputOptions = FileDescriptorOutputOptions.Builder(
            context.contentResolver.openFileDescriptor(uri, "w")!!
        ).build()

        capture.startRecording(outputOptions, cameraExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> { videoState = VideoState.RECORDING }
                is VideoRecordEvent.Finalize -> { videoState = VideoState.IDLE }
                else -> {}
            }
            onEvent(event)
        }
    }

    fun stopVideoRecording() {
        videoCapture?.stopRecording()
        videoState = VideoState.STOPPED
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

    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
