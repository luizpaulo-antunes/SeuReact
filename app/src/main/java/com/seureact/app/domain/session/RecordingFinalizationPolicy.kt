package com.seureact.app.domain.session

import com.seureact.app.domain.model.ProjectStatus

data class RecordingFinalizationDecision(
    val status: ProjectStatus,
    val shouldExport: Boolean,
    val error: String?,
)

object RecordingFinalizationPolicy {
    fun decide(
        exportRequested: Boolean,
        captureError: String?,
        interruptionReason: String?,
    ): RecordingFinalizationDecision {
        if (exportRequested && captureError == null) {
            return RecordingFinalizationDecision(ProjectStatus.READY_TO_EXPORT, true, null)
        }
        return RecordingFinalizationDecision(
            status = ProjectStatus.INTERRUPTED,
            shouldExport = false,
            error = captureError ?: interruptionReason ?: "A gravação foi interrompida antes da finalização.",
        )
    }
}
