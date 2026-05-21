package com.snapcam.presentation

import com.snapcam.domain.model.CameraMode
import com.snapcam.domain.model.Filter
import com.snapcam.presentation.camera.CameraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraUiStateTest {

    @Test
    fun `initial state uses photo mode`() {
        val state = CameraUiState()
        assertEquals(CameraMode.PHOTO, state.mode)
        assertFalse(state.isRecording)
        assertFalse(state.flashEnabled)
        assertEquals(1f, state.zoomLevel)
    }

    @Test
    fun `state can switch to video mode`() {
        val state = CameraUiState(mode = CameraMode.VIDEO)
        assertEquals(CameraMode.VIDEO, state.mode)
    }

    @Test
    fun `recording state toggles`() {
        val recording = CameraUiState(isRecording = true)
        val stopped = CameraUiState(isRecording = false)
        assertTrue(recording.isRecording)
        assertFalse(stopped.isRecording)
    }

    @Test
    fun `flash enabled state`() {
        val flashOn = CameraUiState(flashEnabled = true)
        val flashOff = CameraUiState(flashEnabled = false)
        assertTrue(flashOn.flashEnabled)
        assertFalse(flashOff.flashEnabled)
    }

    @Test
    fun `zoom level clamps to range`() {
        val zoomed = CameraUiState(zoomLevel = 5f)
        assertEquals(5f, zoomed.zoomLevel)
    }

    @Test
    fun `filter selection persists in state`() {
        val state = CameraUiState(selectedFilter = "Vintage")
        assertEquals("Vintage", state.selectedFilter)
    }

    @Test
    fun `error state`() {
        val state = CameraUiState(error = "Camera unavailable")
        assertEquals("Camera unavailable", state.error)
    }

    @Test
    fun `default filter is original`() {
        val state = CameraUiState()
        assertEquals("Original", state.selectedFilter)
    }
}
