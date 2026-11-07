package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
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
}
