package com.snapcam.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val type: MediaType,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val durationMs: Long? = null,
    val filterName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val albumName: String = "SnapCam"
)

enum class MediaType { PHOTO, VIDEO }
