package com.seureact.app.domain.model

import java.util.UUID

enum class ProjectStatus {
    PREPARING, RECORDING, INTERRUPTED, READY_TO_EXPORT, EXPORTING, COMPLETED, FAILED
}

enum class CameraShape { CIRCLE, SQUARE, ROUNDED_RECTANGLE }
enum class CameraFacing { FRONT, BACK }
enum class PointerStyle { FINGER, ARROW, MOUSE }
enum class OutputAspect { ORIGINAL, PORTRAIT_9_16, LANDSCAPE_16_9, SQUARE }
enum class OutputResolution { HD_720, FULL_HD_1080 }

data class CameraTransform(
    val centerX: Float = 0.78f,
    val centerY: Float = 0.22f,
    val width: Float = 0.28f,
    val height: Float = 0.28f,
    val mirrored: Boolean = true,
)

data class SourceTransform(
    val scale: Float = 1f,
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
)

data class PointerMarker(
    val x: Float,
    val y: Float,
    val style: PointerStyle,
)

data class SourceMetadata(
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val hasAudio: Boolean,
    val mimeType: String,
)

data class ReactionProject(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val localSourcePath: String,
    val metadata: SourceMetadata,
    val status: ProjectStatus = ProjectStatus.PREPARING,
    val outputAspect: OutputAspect = OutputAspect.ORIGINAL,
    val outputResolution: OutputResolution = OutputResolution.HD_720,
    val cameraFacing: CameraFacing = CameraFacing.FRONT,
    val cameraShape: CameraShape = CameraShape.CIRCLE,
    val cameraTransform: CameraTransform = CameraTransform(),
    val sourceVolume: Float = 1f,
    val microphoneVolume: Float = 1f,
    val cameraVideoPath: String? = null,
    val microphoneAudioPath: String? = null,
    val outputUri: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = createdAtEpochMs,
    val exportProgress: Int = 0,
    val lastError: String? = null,
)

data class ReactionSession(
    val projectId: String,
    val startedElapsedRealtimeNanos: Long,
    val finishedElapsedRealtimeNanos: Long? = null,
)
