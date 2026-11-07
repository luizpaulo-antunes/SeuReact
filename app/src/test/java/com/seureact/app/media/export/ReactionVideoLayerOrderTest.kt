package com.seureact.app.media.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactionVideoLayerOrderTest {
    @Test
    fun `camera is primary so compositor draws it above source`() {
        assertEquals(
            listOf(ReactionVideoLayer.CAMERA, ReactionVideoLayer.SOURCE),
            ReactionVideoLayerOrder.compositionOrder,
        )
    }

    @Test
    fun `input ids resolve to the configured layers`() {
        assertEquals(ReactionVideoLayer.CAMERA, ReactionVideoLayerOrder.layerForInputId(0))
        assertEquals(ReactionVideoLayer.SOURCE, ReactionVideoLayerOrder.layerForInputId(1))
        assertNull(ReactionVideoLayerOrder.layerForInputId(2))
    }
}
