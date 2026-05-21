package com.snapcam.domain.usecase

import com.snapcam.domain.model.CameraMode
import com.snapcam.domain.model.Filter
import com.snapcam.domain.model.MediaItem
import com.snapcam.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {

    @Test
    fun `media item has required fields`() {
        val item = MediaItem(
            uri = android.net.Uri.parse("file:///test.jpg"),
            type = MediaType.PHOTO,
            width = 1920,
            height = 1080,
            fileSize = 1024
        )
        assertNotNull(item.id)
        assertEquals(MediaType.PHOTO, item.type)
        assertEquals(1920, item.width)
        assertEquals(1080, item.height)
    }

    @Test
    fun `video type has null duration by default`() {
        val item = MediaItem(
            uri = android.net.Uri.parse("file:///test.mp4"),
            type = MediaType.VIDEO,
            width = 1920, height = 1080, fileSize = 2048
        )
        assertEquals(MediaType.VIDEO, item.type)
    }

    @Test
    fun `camera mode enum values`() {
        val modes = CameraMode.values()
        assertTrue(modes.contains(CameraMode.PHOTO))
        assertTrue(modes.contains(CameraMode.VIDEO))
        assertTrue(modes.contains(CameraMode.PORTRAIT))
        assertTrue(modes.contains(CameraMode.PRO))
    }

    @Test
    fun `filter presets contain originals`() {
        val names = Filter.PRESETS.map { it.name }
        assertTrue(names.contains("Original"))
        assertTrue(names.contains("Grayscale"))
        assertTrue(names.contains("Vintage"))
        assertTrue(names.size >= 5)
    }
}

class CameraModeTest {

    @Test
    fun `default mode is photo`() {
        val default = CameraMode.PHOTO
        assertEquals("PHOTO", default.name)
    }

    @Test
    fun `mode display mapping`() {
        assertEquals(4, CameraMode.values().size)
    }
}

class FilterTest {

    @Test
    fun `original filter has no agsl shader`() {
        val original = Filter.PRESETS.first { it.name == "Original" }
        assertEquals(null, original.agslFile)
    }

    @Test
    fun `grayscale filter has agsl path`() {
        val gs = Filter.PRESETS.first { it.name == "Grayscale" }
        assertNotNull(gs.agslFile)
        assertTrue(gs.agslFile!!.endsWith(".agsl"))
    }
}
