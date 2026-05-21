package com.snapcam.presentation.editor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcam.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModelProvider

data class EditorUiState(
    val bitmap: Bitmap? = null,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val rotation: Float = 0f,
    val isCropping: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class EditorViewModel(
    private val mediaRepository: MediaRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    class Factory(
        private val mediaRepository: MediaRepository,
        private val contentResolver: ContentResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(mediaRepository, contentResolver) as T
        }
    }

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var originalBitmap: Bitmap? = null

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val bmp = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                originalBitmap = bmp
                _state.value = _state.value.copy(bitmap = bmp)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load image: ${e.message}"
                )
            }
        }
    }

    fun setBrightness(value: Float) {
        _state.value = _state.value.copy(brightness = value.coerceIn(-1f, 1f))
    }

    fun setContrast(value: Float) {
        _state.value = _state.value.copy(contrast = value.coerceIn(0f, 3f))
    }

    fun setSaturation(value: Float) {
        _state.value = _state.value.copy(saturation = value.coerceIn(0f, 3f))
    }

    fun rotateRight() {
        val newRotation = (_state.value.rotation + 90f) % 360f
        _state.value = _state.value.copy(rotation = newRotation)
    }

    fun toggleCrop() {
        _state.value = _state.value.copy(isCropping = !_state.value.isCropping)
    }

    fun resetAdjustments() {
        _state.value = _state.value.copy(
            brightness = 0f, contrast = 1f, saturation = 1f, rotation = 0f
        )
    }

    private fun applyEdits(): Bitmap? {
        val src = originalBitmap ?: return null
        var working = src.copy(src.config, true)

        if (_state.value.rotation != 0f) {
            val m = Matrix().apply {
                postRotate(_state.value.rotation, working.width / 2f, working.height / 2f)
            }
            working = Bitmap.createBitmap(working, 0, 0, working.width, working.height, m, true)
        }

        val s = _state.value
        if (s.brightness != 0f || s.contrast != 1f || s.saturation != 1f) {
            val result = working.copy(working.config, true)
            val canvas = Canvas(result)
            val paint = Paint()

            val cm = ColorMatrix().apply { setSaturation(s.saturation) }
            val scale = s.contrast
            val translate = s.brightness * 128f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            contrastMatrix.postConcat(cm)
            paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
            canvas.drawBitmap(working, 0f, 0f, paint)
            working = result
        }

        return working
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            try {
                val edited = applyEdits()
                withContext(Dispatchers.IO) {
                    val dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    )
                    dir.mkdirs()
                    val file = File(dir, "EDIT_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        edited?.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                _state.value = _state.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false, error = "Save failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
