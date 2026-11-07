package com.seureact.app.media

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceVideoGeometryTest {
    @Test
    fun `portrait rotation swaps encoded dimensions`() {
        val size = SourceVideoGeometry.orientedSize(1920, 1080, 90)

        assertEquals(1080, size.width)
        assertEquals(1920, size.height)
    }

    @Test
    fun `landscape rotation keeps encoded dimensions`() {
        val size = SourceVideoGeometry.orientedSize(1920, 1080, 0)

        assertEquals(1920, size.width)
        assertEquals(1080, size.height)
    }
}
