package com.seureact.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DualCameraGeometryTest {
    @Test
    fun `camera viewport matches portrait recording canvas`() {
        assertEquals(9, DualCameraGeometry.PORTRAIT_OUTPUT_WIDTH)
        assertEquals(16, DualCameraGeometry.PORTRAIT_OUTPUT_HEIGHT)
        assertEquals(9f / 16f, DualCameraGeometry.PORTRAIT_OUTPUT_ASPECT_RATIO, 0.0001f)
    }

    @Test
    fun `pip uses uniform scale after source crop`() {
        val transform = DualCameraPipTransform(width = 0.32f)

        val composition = DualCameraGeometry.compositionTransform(transform)

        assertEquals(0.32f, composition.scaleX, 0.0001f)
        assertEquals(0.32f, composition.scaleY, 0.0001f)
    }

    @Test
    fun `pip is constrained inside every edge`() {
        val constrained = DualCameraGeometry.constrain(
            DualCameraPipTransform(centerX = 2f, centerY = -1f, width = 1f),
        )
        val height = DualCameraGeometry.pipHeight(constrained)

        assertTrue(constrained.centerX <= 1f - constrained.width / 2f)
        assertTrue(constrained.centerY >= height / 2f)
        assertEquals(0.48f, constrained.width, 0.0001f)
    }

    @Test
    fun `normalized center maps to camera ndc coordinates`() {
        val composition = DualCameraGeometry.compositionTransform(
            DualCameraPipTransform(centerX = 0.75f, centerY = 0.25f),
        )

        assertEquals(0.5f, composition.offsetX, 0.0001f)
        assertEquals(0.5f, composition.offsetY, 0.0001f)
    }
}
