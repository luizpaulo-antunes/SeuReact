package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import com.seureact.app.domain.model.CameraShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class CameraVideoEffectsTest {
    @Test
    fun `camera crop aspect follows selected shape`() {
        assertEquals(1f, cameraCropAspectRatio(CameraShape.CIRCLE), 0f)
        assertEquals(1f, cameraCropAspectRatio(CameraShape.SQUARE), 0f)
        assertEquals(4f / 3f, cameraCropAspectRatio(CameraShape.ROUNDED_RECTANGLE), 0f)
    }

    @Test
    fun `official presentation crop runs before custom mask`() {
        CameraShape.entries.forEach { shape ->
            val effects = cameraVideoEffects(shape)
            assertEquals(2, effects.size)
            assertTrue(effects[0] is Presentation)
            assertTrue(effects[1] is CameraMaskEffect)
        }
    }
}
