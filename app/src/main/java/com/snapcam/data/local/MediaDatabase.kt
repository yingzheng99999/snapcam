package com.snapcam.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.snapcam.domain.model.MediaItem

@Database(entities = [MediaItem::class], version = 1, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
