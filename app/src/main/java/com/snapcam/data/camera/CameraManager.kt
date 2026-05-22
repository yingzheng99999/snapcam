package com.snapcam.data.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
    data class VideoSaved(val uri: android.net.Uri) : CaptureResult()
    data class Error(val message: String, val exception: Throwable? = null) : CaptureResult()
}

class CameraManager(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var activeLens = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val df = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    var isRecording: Boolean = false; private set
    private var mr: MediaRecorder? = null
    private var pendingUri: android.net.Uri? = null
    private var vidCallback: ((CaptureResult) -> Unit)? = null

    fun startCamera(lifecycleOwner: LifecycleOwner, pv: PreviewView, lens: Int = activeLens) {
        ProcessCameraProvider.getInstance(context).addListener({
            cameraProvider?.unbindAll()
            preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG).setTargetRotation(pv.display?.rotation ?: 0).build()
            try {
                camera = ProcessCameraProvider.getInstance(context).get().bindToLifecycle(lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(lens).build(), preview!!, imageCapture!!)
                activeLens = lens
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto(cb: (CaptureResult) -> Unit) {
        val c = imageCapture ?: run { cb(CaptureResult.Error("Not ready")); return }
        val f = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_${df.format(System.currentTimeMillis())}.jpg")
        c.takePicture(ImageCapture.OutputFileOptions.Builder(f).build(), cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(o: ImageCapture.OutputFileResults) { cb(CaptureResult.PhotoSaved(f)) }
                override fun onError(e: ImageCaptureException) { cb(CaptureResult.Error(e.message ?: "Err", e)) }
            })
    }

    fun startVideoRecording(cb: (CaptureResult) -> Unit) {
        val ts = df.format(System.currentTimeMillis())
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "VID_$ts.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv) ?: run { cb(CaptureResult.Error("No URI")); return }
        try {
            mr = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoFrameRate(30); setVideoSize(1280, 720); setVideoEncodingBitRate(4000000); setAudioEncodingBitRate(128000)
                context.contentResolver.openFileDescriptor(uri, "w")?.let { fd -> setOutputFile(fd.fileDescriptor) } ?: run { cb(CaptureResult.Error("No fd")); return }
                prepare(); start()
            }
            isRecording = true; pendingUri = uri; vidCallback = cb
        } catch (e: Exception) { cb(CaptureResult.Error("Record error: ${e.message}")); mr?.release(); mr = null }
    }

    fun stopVideoRecording() {
        try { mr?.apply { stop(); release() } } catch (_: Exception) {}
        mr = null; isRecording = false
        pendingUri?.let { vidCallback?.invoke(CaptureResult.VideoSaved(it)) }; pendingUri = null; vidCallback = null
    }

    fun switchLens(lo: LifecycleOwner, pv: PreviewView) {
        stopVideoRecording()
        startCamera(lo, pv, if (activeLens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
    }

    fun release() { stopVideoRecording(); cameraProvider?.unbindAll(); cameraExecutor.shutdown() }
}
