package com.seureact.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerMarkerLayoutTest {
    @Test
    fun `arrow hotspot remains exact in every screen quadrant`() {
        listOf(
            PointerMarker(0.2f, 0.2f, PointerStyle.ARROW),
            PointerMarker(0.8f, 0.2f, PointerStyle.ARROW),
            PointerMarker(0.2f, 0.8f, PointerStyle.ARROW),
            PointerMarker(0.8f, 0.8f, PointerStyle.ARROW),
        ).forEach { marker ->
            val placement = PointerMarkerLayout.place(marker, 1_000f, 2_000f, 160f)
            val pivotX = placement.left + (placement.right - placement.left) * placement.pivotFractionX
            val pivotY = placement.top + (placement.bottom - placement.top) * placement.pivotFractionY

            assertEquals(marker.x * 1_000f, pivotX, 0.001f)
            assertEquals(marker.y * 2_000f, pivotY, 0.001f)
        }
    }

    @Test
    fun `arrow tail flips toward available space`() {
        val topLeft = PointerMarkerLayout.place(PointerMarker(0.2f, 0.2f, PointerStyle.ARROW), 1f, 1f, 0.2f)
        val bottomRight = PointerMarkerLayout.place(PointerMarker(0.8f, 0.8f, PointerStyle.ARROW), 1f, 1f, 0.2f)

        assertTrue(topLeft.scaleX < 0f)
        assertTrue(topLeft.scaleY > 0f)
        assertTrue(bottomRight.scaleX > 0f)
        assertTrue(bottomRight.scaleY < 0f)
    }

    @Test
    fun `finger and mouse remain centered on touched hotspot`() {
        listOf(PointerStyle.FINGER, PointerStyle.MOUSE).forEach { style ->
            val marker = PointerMarker(0.35f, 0.7f, style)
            val placement = PointerMarkerLayout.place(marker, 800f, 1_200f, 96f)

            assertEquals(0.5f, placement.pivotFractionX, 0f)
            assertEquals(0.5f, placement.pivotFractionY, 0f)
            assertEquals(marker.x * 800f, placement.left + 96f * placement.pivotFractionX, 0.001f)
            assertEquals(marker.y * 1_200f, placement.top + 96f * placement.pivotFractionY, 0.001f)
        }
    }
}
