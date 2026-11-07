package com.seureact.app.data

import com.seureact.app.data.local.ProjectEntity
import com.seureact.app.data.local.TimelineEventEntity
import com.seureact.app.domain.model.CameraFacing
import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.OutputAspect
import com.seureact.app.domain.model.OutputResolution
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.SourceMetadata
import com.seureact.app.domain.model.TimelineEvent

fun ReactionProject.toEntity() = ProjectEntity(
    id, sourceUri, localSourcePath, metadata.displayName, metadata.durationMs, metadata.width,
    metadata.height, metadata.hasAudio, metadata.mimeType, status.name, outputAspect.name,
    outputResolution.name, cameraFacing.name, cameraShape.name, cameraTransform.centerX,
    cameraTransform.centerY, cameraTransform.width, cameraTransform.height, cameraTransform.mirrored,
    sourceVolume, microphoneVolume, cameraVideoPath, microphoneAudioPath, outputUri,
    createdAtEpochMs, updatedAtEpochMs, exportProgress, lastError,
)

fun ProjectEntity.toDomain() = ReactionProject(
    id = id,
    sourceUri = sourceUri,
    localSourcePath = localSourcePath,
    metadata = SourceMetadata(sourceName, sourceDurationMs, sourceWidth, sourceHeight, sourceHasAudio, sourceMimeType),
    status = ProjectStatus.valueOf(status),
    outputAspect = OutputAspect.valueOf(outputAspect),
    outputResolution = OutputResolution.valueOf(outputResolution),
    cameraFacing = CameraFacing.valueOf(cameraFacing),
    cameraShape = CameraShape.valueOf(cameraShape),
    cameraTransform = CameraTransform(cameraCenterX, cameraCenterY, cameraWidth, cameraHeight, cameraMirrored),
    sourceVolume = sourceVolume,
    microphoneVolume = microphoneVolume,
    cameraVideoPath = cameraVideoPath,
    microphoneAudioPath = microphoneAudioPath,
    outputUri = outputUri,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    exportProgress = exportProgress,
    lastError = lastError,
)

fun TimelineEvent.toEntity(projectId: String): TimelineEventEntity = when (this) {
    is TimelineEvent.SessionStarted -> TimelineEventEntity(
        projectId = projectId, type = "SESSION_STARTED", finalTimeNanos = finalTimeNanos,
        sourcePositionMs = sourcePositionMs, booleanValue = sourcePlaying,
        floatValue = sourceVolume, secondaryFloatValue = microphoneVolume,
        centerX = cameraTransform.centerX, centerY = cameraTransform.centerY,
        width = cameraTransform.width, height = cameraTransform.height,
        mirrored = cameraTransform.mirrored, cameraShape = cameraShape.name, cameraFacing = cameraFacing.name,
    )
    is TimelineEvent.SourcePlay -> base(projectId, "SOURCE_PLAY")
    is TimelineEvent.SourcePause -> base(projectId, "SOURCE_PAUSE")
    is TimelineEvent.SourceSeek -> base(projectId, "SOURCE_SEEK")
    is TimelineEvent.SourceVolumeChanged -> base(projectId, "SOURCE_VOLUME", floatValue = volume)
    is TimelineEvent.SourceMuted -> base(projectId, "SOURCE_MUTED", booleanValue = muted)
    is TimelineEvent.MicrophoneVolumeChanged -> base(projectId, "MICROPHONE_VOLUME", floatValue = volume)
    is TimelineEvent.CameraTransformChanged -> base(
        projectId, "CAMERA_TRANSFORM", centerX = transform.centerX, centerY = transform.centerY,
        width = transform.width, height = transform.height, mirrored = transform.mirrored,
    )
    is TimelineEvent.CameraShapeChanged -> base(projectId, "CAMERA_SHAPE", cameraShape = shape.name)
    is TimelineEvent.CameraSwitched -> base(projectId, "CAMERA_SWITCHED", cameraFacing = facing.name)
    is TimelineEvent.SessionFinished -> base(projectId, "SESSION_FINISHED")
}

private fun TimelineEvent.base(
    projectId: String,
    type: String,
    booleanValue: Boolean? = null,
    floatValue: Float? = null,
    centerX: Float? = null,
    centerY: Float? = null,
    width: Float? = null,
    height: Float? = null,
    mirrored: Boolean? = null,
    cameraShape: String? = null,
    cameraFacing: String? = null,
) = TimelineEventEntity(
    projectId = projectId, type = type, finalTimeNanos = finalTimeNanos,
    sourcePositionMs = sourcePositionMs, booleanValue = booleanValue, floatValue = floatValue,
    centerX = centerX, centerY = centerY, width = width, height = height, mirrored = mirrored,
    cameraShape = cameraShape, cameraFacing = cameraFacing,
)

fun TimelineEventEntity.toDomain(): TimelineEvent = when (type) {
    "SESSION_STARTED" -> TimelineEvent.SessionStarted(
        finalTimeNanos, sourcePositionMs, booleanValue ?: true, floatValue ?: 1f,
        secondaryFloatValue ?: 1f, transform(), CameraShape.valueOf(cameraShape!!),
        CameraFacing.valueOf(cameraFacing!!),
    )
    "SOURCE_PLAY" -> TimelineEvent.SourcePlay(finalTimeNanos, sourcePositionMs)
    "SOURCE_PAUSE" -> TimelineEvent.SourcePause(finalTimeNanos, sourcePositionMs)
    "SOURCE_SEEK" -> TimelineEvent.SourceSeek(finalTimeNanos, sourcePositionMs)
    "SOURCE_VOLUME" -> TimelineEvent.SourceVolumeChanged(finalTimeNanos, sourcePositionMs, floatValue!!)
    "SOURCE_MUTED" -> TimelineEvent.SourceMuted(finalTimeNanos, sourcePositionMs, booleanValue!!)
    "MICROPHONE_VOLUME" -> TimelineEvent.MicrophoneVolumeChanged(finalTimeNanos, sourcePositionMs, floatValue!!)
    "CAMERA_TRANSFORM" -> TimelineEvent.CameraTransformChanged(finalTimeNanos, sourcePositionMs, transform())
    "CAMERA_SHAPE" -> TimelineEvent.CameraShapeChanged(finalTimeNanos, sourcePositionMs, CameraShape.valueOf(cameraShape!!))
    "CAMERA_SWITCHED" -> TimelineEvent.CameraSwitched(finalTimeNanos, sourcePositionMs, CameraFacing.valueOf(cameraFacing!!))
    "SESSION_FINISHED" -> TimelineEvent.SessionFinished(finalTimeNanos, sourcePositionMs)
    else -> error("Tipo de evento desconhecido: $type")
}

private fun TimelineEventEntity.transform() = CameraTransform(
    centerX ?: 0.78f, centerY ?: 0.22f, width ?: 0.28f, height ?: 0.28f, mirrored ?: true,
)
