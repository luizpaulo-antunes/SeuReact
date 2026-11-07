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
        val decision = RecordingFinalizationPolicy.decide(true, null, null)

        assertEquals(ProjectStatus.READY_TO_EXPORT, decision.status)
        assertTrue(decision.shouldExport)
        assertNull(decision.error)
    }

    @Test
    fun `camera finalize error never leaves project recording`() {
        val decision = RecordingFinalizationPolicy.decide(true, "Falha da câmera", null)

        assertEquals(ProjectStatus.INTERRUPTED, decision.status)
        assertFalse(decision.shouldExport)
        assertEquals("Falha da câmera", decision.error)
    }

    @Test
    fun `lifecycle interruption remains recoverable`() {
        val decision = RecordingFinalizationPolicy.decide(false, null, "Aplicativo saiu do primeiro plano")

        assertEquals(ProjectStatus.INTERRUPTED, decision.status)
        assertFalse(decision.shouldExport)
        assertEquals("Aplicativo saiu do primeiro plano", decision.error)
    }
}
