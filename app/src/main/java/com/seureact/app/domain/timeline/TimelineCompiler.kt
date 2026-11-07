package com.seureact.app.domain.timeline

import com.seureact.app.domain.model.CameraFacing
import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.PointerMarker
import com.seureact.app.domain.model.SourceTransform
import com.seureact.app.domain.model.TimelineEvent

enum class SourceRenderMode { PLAYING, FROZEN }

data class RenderSegment(
    val finalStartNanos: Long,
    val finalEndNanos: Long,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val sourceMode: SourceRenderMode,
    val cameraStartNanos: Long,
    val cameraEndNanos: Long,
    val cameraTransform: CameraTransform,
    val cameraShape: CameraShape,
    val cameraFacing: CameraFacing,
    val sourceMuted: Boolean,
    val sourceVolume: Float,
    val microphoneVolume: Float,
    val sourceTransform: SourceTransform,
    val pointerMarker: PointerMarker?,
) {
    val durationNanos: Long get() = finalEndNanos - finalStartNanos
}

class TimelineCompiler {
    fun compile(events: List<TimelineEvent>): List<RenderSegment> {
        require(events.isNotEmpty()) { "A timeline precisa de eventos" }
        val ordered = events.withIndex()
            .sortedWith(compareBy<IndexedValue<TimelineEvent>> { it.value.finalTimeNanos }.thenBy { it.index })
            .map { it.value }
        val start = ordered.firstOrNull { it is TimelineEvent.SessionStarted } as? TimelineEvent.SessionStarted
            ?: error("SessionStarted ausente")
        require(start.finalTimeNanos == 0L) { "SessionStarted deve começar em zero" }

        var state = State.from(start)
        val segments = mutableListOf<RenderSegment>()
        var cursorNanos = 0L

        for (event in ordered.dropWhile { it !== start }.drop(1)) {
            require(event.finalTimeNanos >= cursorNanos) { "Eventos fora de ordem" }
            if (event.finalTimeNanos > cursorNanos) {
                val elapsedNanos = event.finalTimeNanos - cursorNanos
                val sourceEndMs = if (state.playing) {
                    state.sourcePositionMs + elapsedNanos / NANOS_PER_MILLISECOND
                } else {
                    state.sourcePositionMs
                }
                segments += RenderSegment(
                    finalStartNanos = cursorNanos,
                    finalEndNanos = event.finalTimeNanos,
                    sourceStartMs = state.sourcePositionMs,
                    sourceEndMs = sourceEndMs,
                    sourceMode = if (state.playing) SourceRenderMode.PLAYING else SourceRenderMode.FROZEN,
                    cameraStartNanos = cursorNanos,
                    cameraEndNanos = event.finalTimeNanos,
                    cameraTransform = state.transform,
                    cameraShape = state.shape,
                    cameraFacing = state.facing,
                    sourceMuted = state.muted,
                    sourceVolume = state.sourceVolume,
                    microphoneVolume = state.microphoneVolume,
                    sourceTransform = state.sourceTransform,
                    pointerMarker = state.pointerMarker,
                )
                state = state.copy(sourcePositionMs = sourceEndMs)
            }
            state = state.apply(event)
            cursorNanos = event.finalTimeNanos
            if (event is TimelineEvent.SessionFinished) break
        }
        require(ordered.any { it is TimelineEvent.SessionFinished }) { "SessionFinished ausente" }
        return segments
    }

    private data class State(
        val playing: Boolean,
        val sourcePositionMs: Long,
        val sourceVolume: Float,
        val microphoneVolume: Float,
        val muted: Boolean,
        val transform: CameraTransform,
        val shape: CameraShape,
        val facing: CameraFacing,
        val sourceTransform: SourceTransform,
        val pointerMarker: PointerMarker?,
    ) {
        fun apply(event: TimelineEvent): State = when (event) {
            is TimelineEvent.SessionStarted -> from(event)
            is TimelineEvent.SourcePlay -> copy(playing = true, sourcePositionMs = event.sourcePositionMs)
            is TimelineEvent.SourcePause -> copy(playing = false, sourcePositionMs = event.sourcePositionMs)
            is TimelineEvent.SourceSeek -> copy(sourcePositionMs = event.sourcePositionMs)
            is TimelineEvent.SourceVolumeChanged -> copy(sourcePositionMs = event.sourcePositionMs, sourceVolume = event.volume.coerceIn(0f, 1f))
            is TimelineEvent.SourceMuted -> copy(sourcePositionMs = event.sourcePositionMs, muted = event.muted)
            is TimelineEvent.MicrophoneVolumeChanged -> copy(sourcePositionMs = event.sourcePositionMs, microphoneVolume = event.volume.coerceIn(0f, 2f))
            is TimelineEvent.CameraTransformChanged -> copy(sourcePositionMs = event.sourcePositionMs, transform = event.transform)
            is TimelineEvent.CameraShapeChanged -> copy(sourcePositionMs = event.sourcePositionMs, shape = event.shape)
            is TimelineEvent.CameraSwitched -> copy(sourcePositionMs = event.sourcePositionMs, facing = event.facing)
            is TimelineEvent.SourceTransformChanged -> copy(sourcePositionMs = event.sourcePositionMs, sourceTransform = event.transform)
            is TimelineEvent.PointerPlaced -> copy(sourcePositionMs = event.sourcePositionMs, pointerMarker = event.marker)
            is TimelineEvent.PointerCleared -> copy(sourcePositionMs = event.sourcePositionMs, pointerMarker = null)
            is TimelineEvent.SessionFinished -> copy(sourcePositionMs = event.sourcePositionMs)
        }

        companion object {
            fun from(event: TimelineEvent.SessionStarted) = State(
                playing = event.sourcePlaying,
                sourcePositionMs = event.sourcePositionMs,
                sourceVolume = event.sourceVolume.coerceIn(0f, 1f),
                microphoneVolume = event.microphoneVolume.coerceIn(0f, 2f),
                muted = false,
                transform = event.cameraTransform,
                shape = event.cameraShape,
                facing = event.cameraFacing,
                sourceTransform = event.sourceTransform,
                pointerMarker = null,
            )
        }
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
