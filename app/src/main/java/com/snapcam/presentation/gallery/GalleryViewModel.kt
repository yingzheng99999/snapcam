package com.snapcam.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val items = mediaRepository.getAll()
            _state.value = GalleryUiState(items = items, isLoading = false)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            mediaRepository.delete(id)
            load()
        }
    }
}
