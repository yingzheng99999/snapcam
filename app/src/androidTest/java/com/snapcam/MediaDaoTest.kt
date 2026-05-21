package com.snapcam.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaDaoTest {

    private lateinit var db: MediaDatabase
    private lateinit var dao: MediaDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MediaDatabase::class.java
        ).build()
        dao = db.mediaDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and retrieve media item`() = runBlocking {
        val item = MediaItem(
            uri = android.net.Uri.parse("file:///photo.jpg"),
            type = MediaType.PHOTO,
            width = 1920, height = 1080, fileSize = 500000
        )
        dao.insert(item)
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(item.id, all[0].id)
    }

    @Test
    fun `insert multiple items returns in reverse chronological order`() = runBlocking {
        val item1 = MediaItem(
            id = "1", uri = android.net.Uri.parse("file:///a.jpg"),
            type = MediaType.PHOTO, width = 100, height = 100, fileSize = 100,
            createdAt = 1000
        )
        val item2 = MediaItem(
            id = "2", uri = android.net.Uri.parse("file:///b.jpg"),
            type = MediaType.PHOTO, width = 200, height = 200, fileSize = 200,
            createdAt = 2000
        )
        dao.insert(item1)
        dao.insert(item2)
        val all = dao.getAll()
        assertEquals(2, all.size)
        assertEquals("2", all[0].id)
    }

    @Test
    fun `delete item removes it from database`() = runBlocking {
        val item = MediaItem(
            uri = android.net.Uri.parse("file:///delete.jpg"),
            type = MediaType.PHOTO, width = 100, height = 100, fileSize = 100
        )
        dao.insert(item)
        dao.deleteById(item.id)
        val result = dao.getById(item.id)
        assertEquals(null, result)
    }

    @Test
    fun `get item by id returns correct item`() = runBlocking {
        val item = MediaItem(
            id = "findme", uri = android.net.Uri.parse("file:///find.jpg"),
            type = MediaType.PHOTO, width = 100, height = 100, fileSize = 100
        )
        dao.insert(item)
        val found = dao.getById("findme")
        assertTrue(found != null)
        assertEquals("findme", found!!.id)
    }

    @Test
    fun `flow emits inserted items`() = runBlocking {
        val item = MediaItem(
            uri = android.net.Uri.parse("file:///flow.jpg"),
            type = MediaType.PHOTO, width = 100, height = 100, fileSize = 100
        )
        dao.insert(item)
        val emitted = dao.getAllFlow().first()
        assertTrue(emitted.isNotEmpty())
        assertEquals(1, emitted.size)
    }
}
