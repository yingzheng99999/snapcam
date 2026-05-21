package com.snapcam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY createdAt DESC")
    suspend fun getByType(type: MediaType): List<MediaItem>

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()
}
