package com.seureact.app.media.export

import android.net.Uri
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.timeline.RenderSegment

data class ExportResult(val outputUri: Uri, val durationMs: Long)

interface ReactionExporter {
    suspend fun export(
        project: ReactionProject,
        segments: List<RenderSegment>,
        onProgress: suspend (Int) -> Unit,
    ): ExportResult
}
