package com.seureact.app.domain.model

sealed interface TimelineEvent {
    val finalTimeNanos: Long
    val sourcePositionMs: Long

    data class SessionStarted(
        override val finalTimeNanos: Long = 0,
        override val sourcePositionMs: Long = 0,
        val sourcePlaying: Boolean = true,
        val sourceVolume: Float = 1f,
        val microphoneVolume: Float = 1f,
        val cameraTransform: CameraTransform = CameraTransform(),
        val cameraShape: CameraShape = CameraShape.CIRCLE,
        val cameraFacing: CameraFacing = CameraFacing.FRONT,
        val sourceTransform: SourceTransform = SourceTransform(),
    ) : TimelineEvent

    data class SourcePlay(override val finalTimeNanos: Long, override val sourcePositionMs: Long) : TimelineEvent
    data class SourcePause(override val finalTimeNanos: Long, override val sourcePositionMs: Long) : TimelineEvent
    data class SourceSeek(override val finalTimeNanos: Long, override val sourcePositionMs: Long) : TimelineEvent
    data class SourceVolumeChanged(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val volume: Float,
    ) : TimelineEvent
    data class SourceMuted(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val muted: Boolean,
    ) : TimelineEvent
    data class MicrophoneVolumeChanged(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val volume: Float,
    ) : TimelineEvent
    data class CameraTransformChanged(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val transform: CameraTransform,
    ) : TimelineEvent
    data class CameraShapeChanged(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val shape: CameraShape,
    ) : TimelineEvent
    data class CameraSwitched(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val facing: CameraFacing,
    ) : TimelineEvent
    data class SourceTransformChanged(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val transform: SourceTransform,
    ) : TimelineEvent
    data class PointerPlaced(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
        val marker: PointerMarker,
    ) : TimelineEvent
    data class PointerCleared(
        override val finalTimeNanos: Long,
        override val sourcePositionMs: Long,
    ) : TimelineEvent
    data class SessionFinished(override val finalTimeNanos: Long, override val sourcePositionMs: Long) : TimelineEvent
}
