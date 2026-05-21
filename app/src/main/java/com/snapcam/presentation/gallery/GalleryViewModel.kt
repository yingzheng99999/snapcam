package com.snapcam.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true
)

class GalleryViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    class Factory(private val mediaRepository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(mediaRepository) as T
        }
    }

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
