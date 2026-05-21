package com.snapcam.domain.model

import android.net.Uri

data class MediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
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
