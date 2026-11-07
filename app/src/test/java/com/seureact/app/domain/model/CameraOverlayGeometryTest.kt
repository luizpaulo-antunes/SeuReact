package com.seureact.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraOverlayGeometryTest {
    @Test
    fun `circle uses equal physical width and height on portrait output`() {
        val size = CameraOverlayGeometry.normalizedSize(CameraTransform(width = 0.28f), CameraShape.CIRCLE, 9f / 16f)

        assertEquals(0.28f, size.width, 0.0001f)
        assertEquals(0.1575f, size.height, 0.0001f)
        assertEquals(size.width * 720f, size.height * 1280f, 0.01f)
    }

    @Test
    fun `circle remains inside landscape output after constraint`() {
        val transform = CameraOverlayGeometry.constrain(
            CameraTransform(centerX = 0.05f, centerY = 0.05f, width = 0.6f),
            CameraShape.CIRCLE,
            16f / 9f,
        )
        val size = CameraOverlayGeometry.normalizedSize(transform, CameraShape.CIRCLE, 16f / 9f)

        assertTrue(transform.centerX >= size.width / 2f)
        assertTrue(transform.centerY >= size.height / 2f)
        assertTrue(transform.centerX <= 1f - size.width / 2f)
        assertTrue(transform.centerY <= 1f - size.height / 2f)
    }
}
