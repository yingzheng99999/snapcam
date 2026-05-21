package com.snapcam.presentation.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcam.data.camera.CameraManager
import com.snapcam.data.camera.CaptureResult
import com.snapcam.data.camera.VideoState
import com.snapcam.domain.model.CameraMode
import com.snapcam.domain.model.Filter
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType
import com.snapcam.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val mode: CameraMode = CameraMode.PHOTO,
    val isRecording: Boolean = false,
    val flashEnabled: Boolean = false,
    val zoomLevel: Float = 1f,
    val selectedFilter: String = "Original",
    val lensFacing: Int = 0,
    val latestUri: Uri? = null,
    val showFilterCarousel: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setMode(mode: CameraMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(
            flashEnabled = !_uiState.value.flashEnabled
        )
    }

    fun setZoom(level: Float) {
        _uiState.value = _uiState.value.copy(zoomLevel = level.coerceIn(1f, 8f))
    }

    fun selectFilter(name: String) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = name,
            showFilterCarousel = false
        )
    }

    fun toggleFilterCarousel() {
        _uiState.value = _uiState.value.copy(
            showFilterCarousel = !_uiState.value.showFilterCarousel
        )
    }

    fun switchLens() {
        _uiState.value = _uiState.value.copy(
            lensFacing = if (_uiState.value.lensFacing == 0) 1 else 0
        )
    }

    fun capturePhoto() {
        cameraManager.capturePhoto { result ->
            when (result) {
                is CaptureResult.PhotoSaved -> {
                    val item = MediaItem(
                        uri = Uri.fromFile(result.file),
                        type = MediaType.PHOTO,
                        width = 0, height = 0,
                        fileSize = result.file.length(),
                        filterName = _uiState.value.selectedFilter
                    )
                    viewModelScope.launch {
                        mediaRepository.save(item)
                        _uiState.value = _uiState.value.copy(latestUri = item.uri)
                    }
                }
                is CaptureResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun toggleVideo() {
        if (_uiState.value.isRecording) {
            cameraManager.stopVideoRecording()
            _uiState.value = _uiState.value.copy(isRecording = false)
        } else {
            cameraManager.startVideoRecording { event ->
                when (event) {
                    is androidx.camera.video.VideoRecordEvent.Finalize -> {
                        event.outputResults.outputUri?.let { uri ->
                            val item = MediaItem(
                                uri = uri, type = MediaType.VIDEO,
                                width = 0, height = 0, fileSize = 0,
                                durationMs = event.recordingDuration
                            )
                            viewModelScope.launch {
                                mediaRepository.save(item)
                                _uiState.value = _uiState.value.copy(latestUri = uri)
                            }
                        }
                    }
                    else -> {}
                }
            }
            _uiState.value = _uiState.value.copy(isRecording = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
