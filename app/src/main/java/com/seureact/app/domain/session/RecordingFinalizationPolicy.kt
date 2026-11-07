package com.seureact.app.domain.session

import com.seureact.app.domain.model.ProjectStatus

data class RecordingFinalizationDecision(
    val status: ProjectStatus?,
    val shouldExport: Boolean,
    val shouldDiscard: Boolean,
    val error: String?,
)

enum class RecordingCompletionIntent { EXPORT, INTERRUPT, DISCARD }

object RecordingFinalizationPolicy {
    fun decide(
        intent: RecordingCompletionIntent,
        captureError: String?,
        interruptionReason: String?,
    ): RecordingFinalizationDecision {
        if (intent == RecordingCompletionIntent.DISCARD) {
            return RecordingFinalizationDecision(null, false, true, null)
        }
        if (intent == RecordingCompletionIntent.EXPORT && captureError == null) {
            return RecordingFinalizationDecision(ProjectStatus.READY_TO_EXPORT, true, false, null)
        }
        return RecordingFinalizationDecision(
            status = ProjectStatus.INTERRUPTED,
            shouldExport = false,
            shouldDiscard = false,
            error = captureError ?: interruptionReason ?: "A gravação foi interrompida antes da finalização.",
        )
    }
}
