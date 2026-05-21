package com.snapcam.domain.repository

import android.net.Uri
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType

interface MediaRepository {
    suspend fun save(media: MediaItem)
    suspend fun delete(id: String)
    suspend fun getAll(): List<MediaItem>
    suspend fun getByType(type: MediaType): List<MediaItem>
    suspend fun getById(id: String): MediaItem?
}
