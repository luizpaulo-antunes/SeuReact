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
    fun `circle and square crop camera input to square before masking`() {
        assertEquals(Size(1080, 1080), CameraMaskEffect(CameraShape.CIRCLE).outputSize(1920, 1080))
        assertEquals(Size(1080, 1080), CameraMaskEffect(CameraShape.SQUARE).outputSize(1920, 1080))
        assertEquals(Size(1440, 1080), CameraMaskEffect(CameraShape.ROUNDED_RECTANGLE).outputSize(1920, 1080))
    }
}
