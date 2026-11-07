package com.seureact.app.domain.session

import com.seureact.app.domain.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFinalizationPolicyTest {
    @Test
    fun `successful user finish becomes ready to export`() {
        val decision = RecordingFinalizationPolicy.decide(RecordingCompletionIntent.EXPORT, null, null)

        assertEquals(ProjectStatus.READY_TO_EXPORT, decision.status)
        assertTrue(decision.shouldExport)
        assertFalse(decision.shouldDiscard)
        assertNull(decision.error)
    }

    @Test
    fun `camera finalize error never leaves project recording`() {
        val decision = RecordingFinalizationPolicy.decide(RecordingCompletionIntent.EXPORT, "Falha da câmera", null)

        assertEquals(ProjectStatus.INTERRUPTED, decision.status)
        assertFalse(decision.shouldExport)
        assertFalse(decision.shouldDiscard)
        assertEquals("Falha da câmera", decision.error)
    }

    @Test
    fun `lifecycle interruption remains recoverable`() {
        val decision = RecordingFinalizationPolicy.decide(
            RecordingCompletionIntent.INTERRUPT,
            null,
            "Aplicativo saiu do primeiro plano",
        )

        assertEquals(ProjectStatus.INTERRUPTED, decision.status)
        assertFalse(decision.shouldExport)
        assertFalse(decision.shouldDiscard)
        assertEquals("Aplicativo saiu do primeiro plano", decision.error)
    }

    @Test
    fun `explicit discard never exports or preserves project`() {
        val decision = RecordingFinalizationPolicy.decide(RecordingCompletionIntent.DISCARD, null, null)

        assertNull(decision.status)
        assertFalse(decision.shouldExport)
        assertTrue(decision.shouldDiscard)
        assertNull(decision.error)
    }
}
