package com.example.uptesttaskkotlin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeGeometryTest {

    // Top-left quarter of a 640x480 landscape camera buffer.
    private val quarter = PixelRect(0, 0, 320, 240)

    @Test
    fun `toUpright keeps the rectangle when the buffer is not rotated`() {
        assertEquals(quarter, quarter.toUpright(BUFFER_WIDTH, BUFFER_HEIGHT, 0))
    }

    @Test
    fun `toUpright moves the top-left quarter to the top-right for 90 degrees`() {
        assertEquals(PixelRect(240, 0, 480, 320), quarter.toUpright(BUFFER_WIDTH, BUFFER_HEIGHT, 90))
    }

    @Test
    fun `toUpright moves the top-left quarter to the bottom-right for 180 degrees`() {
        assertEquals(PixelRect(320, 240, 640, 480), quarter.toUpright(BUFFER_WIDTH, BUFFER_HEIGHT, 180))
    }

    @Test
    fun `toUpright moves the top-left quarter to the bottom-left for 270 degrees`() {
        assertEquals(PixelRect(0, 320, 240, 640), quarter.toUpright(BUFFER_WIDTH, BUFFER_HEIGHT, 270))
    }

    @Test
    fun `box that fills the visible area is normalized to the unit rectangle`() {
        val visibleArea = PixelRect(0, 80, 480, 560)

        assertEquals(NormalizedRect(0f, 0f, 1f, 1f), NormalizedRect.of(visibleArea, visibleArea))
    }

    @Test
    fun `box is normalized relative to the visible area, not the whole image`() {
        val visibleArea = PixelRect(0, 80, 480, 560)
        val box = PixelRect(120, 200, 360, 440)

        assertEquals(NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f), NormalizedRect.of(box, visibleArea))
    }

    @Test
    fun `box outside the visible area gets coordinates beyond the unit rectangle`() {
        val visibleArea = PixelRect(0, 80, 480, 560)
        val box = PixelRect(-48, 32, 528, 608)

        val normalized = NormalizedRect.of(box, visibleArea)

        assertTrue(normalized.left < 0f && normalized.top < 0f)
        assertTrue(normalized.right > 1f && normalized.bottom > 1f)
    }

    private companion object {
        const val BUFFER_WIDTH = 640
        const val BUFFER_HEIGHT = 480
    }
}
