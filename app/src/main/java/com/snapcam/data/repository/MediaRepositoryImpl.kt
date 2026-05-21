package com.snapcam.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.snapcam.data.local.MediaDao
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType
import com.snapcam.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val contentResolver: ContentResolver
) : MediaRepository {

    override suspend fun save(media: MediaItem) = mediaDao.insert(media)

    override suspend fun delete(id: String) {
        val item = mediaDao.getById(id)
        if (item != null) {
            contentResolver.delete(Uri.parse(item.uri), null, null)
            mediaDao.deleteById(id)
        }
    }

    override suspend fun getAll(): List<MediaItem> = mediaDao.getAll()

    override suspend fun getByType(type: MediaType): List<MediaItem> = mediaDao.getByType(type)

    override suspend fun getById(id: String): MediaItem? = mediaDao.getById(id)

    fun getAllFlow(): Flow<List<MediaItem>> = mediaDao.getAllFlow()
}
