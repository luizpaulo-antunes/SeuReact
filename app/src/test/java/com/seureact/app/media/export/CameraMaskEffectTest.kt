package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Size
import com.seureact.app.domain.model.CameraShape
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class CameraMaskEffectTest {
    @Test
    fun `camera shapes map to distinct shader uniforms`() {
        assertEquals(0, CameraMaskEffect(CameraShape.CIRCLE).shapeUniform)
        assertEquals(1, CameraMaskEffect(CameraShape.SQUARE).shapeUniform)
        assertEquals(2, CameraMaskEffect(CameraShape.ROUNDED_RECTANGLE).shapeUniform)
    }

    @Test
    fun `mask never changes frame dimensions`() {
        CameraShape.entries.forEach { shape ->
            assertEquals(Size(1920, 1080), CameraMaskEffect(shape).outputSize(1920, 1080))
        }
    }

    @Test
    fun `portrait camera crops vertically without changing frame size`() {
        CameraShape.entries.forEach { shape ->
            val scale = CameraMaskEffect(shape).cropScale(1_080, 1_920)
            val expectedY = when (shape) {
                CameraShape.CIRCLE, CameraShape.SQUARE -> 0.5625f
                CameraShape.ROUNDED_RECTANGLE -> 0.421875f
            }

            assertEquals(1f, scale[0], 0f)
            assertEquals(expectedY, scale[1], 0.000001f)
        }
    }

    @Test
    fun `landscape camera crops horizontally to selected shape`() {
        val circle = CameraMaskEffect(CameraShape.CIRCLE).cropScale(1_920, 1_080)
        val rounded = CameraMaskEffect(CameraShape.ROUNDED_RECTANGLE).cropScale(1_920, 1_080)

        assertEquals(0.5625f, circle[0], 0.000001f)
        assertEquals(1f, circle[1], 0f)
        assertEquals(0.75f, rounded[0], 0.000001f)
        assertEquals(1f, rounded[1], 0f)
    }
}
