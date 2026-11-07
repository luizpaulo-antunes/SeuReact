package com.seureact.app.domain.timeline

import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.TimelineEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCompilerTest {
    private val compiler = TimelineCompiler()

    @Test
    fun `playing event becomes source interval`() {
        val segments = compiler.compile(listOf(start(), finish(3_000, 3_000)))
        assertEquals(1, segments.size)
        assertEquals(0, segments.single().sourceStartMs)
        assertEquals(3_000, segments.single().sourceEndMs)
        assertEquals(SourceRenderMode.PLAYING, segments.single().sourceMode)
    }

    @Test
    fun `pause freezes source while final timeline advances`() {
        val segments = compiler.compile(listOf(
            start(),
            TimelineEvent.SourcePause(ms(2_000), 2_000),
            TimelineEvent.SourcePlay(ms(7_000), 2_000),
            finish(9_000, 4_000),
        ))
        assertEquals(listOf(SourceRenderMode.PLAYING, SourceRenderMode.FROZEN, SourceRenderMode.PLAYING), segments.map { it.sourceMode })
        assertEquals(5_000, segments[1].durationNanos / 1_000_000)
        assertEquals(2_000, segments[1].sourceStartMs)
        assertEquals(2_000, segments[1].sourceEndMs)
        assertEquals(9_000, segments.sumOf { it.durationNanos } / 1_000_000)
    }

    @Test
    fun `forward seek starts next segment at target`() {
        val segments = compiler.compile(listOf(start(), TimelineEvent.SourceSeek(ms(2_000), 10_000), finish(4_000, 12_000)))
        assertEquals(2_000, segments[0].sourceEndMs)
        assertEquals(10_000, segments[1].sourceStartMs)
        assertEquals(12_000, segments[1].sourceEndMs)
    }

    @Test
    fun `backward seek preserves repeated content`() {
        val segments = compiler.compile(listOf(start(), TimelineEvent.SourceSeek(ms(5_000), 2_000), finish(8_000, 5_000)))
        assertEquals(5_000, segments[0].sourceEndMs)
        assertEquals(2_000, segments[1].sourceStartMs)
        assertEquals(5_000, segments[1].sourceEndMs)
        assertEquals(8_000, segments.sumOf { it.durationNanos } / 1_000_000)
    }

    @Test
    fun `mute and volume changes apply only after event`() {
        val segments = compiler.compile(listOf(
            start(),
            TimelineEvent.SourceVolumeChanged(ms(1_000), 1_000, 0.4f),
            TimelineEvent.SourceMuted(ms(2_000), 2_000, true),
            finish(3_000, 3_000),
        ))
        assertEquals(1f, segments[0].sourceVolume)
        assertEquals(0.4f, segments[1].sourceVolume)
        assertFalse(segments[1].sourceMuted)
        assertTrue(segments[2].sourceMuted)
    }

    @Test
    fun `camera transform and shape are retained by segment`() {
        val transform = CameraTransform(0.2f, 0.7f, 0.4f, 0.3f, false)
        val segments = compiler.compile(listOf(
            start(),
            TimelineEvent.CameraTransformChanged(ms(1_000), 1_000, transform),
            TimelineEvent.CameraShapeChanged(ms(2_000), 2_000, CameraShape.ROUNDED_RECTANGLE),
            finish(3_000, 3_000),
        ))
        assertEquals(transform, segments[1].cameraTransform)
        assertEquals(CameraShape.ROUNDED_RECTANGLE, segments[2].cameraShape)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `session without finish is rejected`() {
        compiler.compile(listOf(start(), TimelineEvent.SourcePause(ms(1_000), 1_000)))
    }

    private fun start() = TimelineEvent.SessionStarted()
    private fun finish(finalMs: Long, sourceMs: Long) = TimelineEvent.SessionFinished(ms(finalMs), sourceMs)
    private fun ms(value: Long) = value * 1_000_000L
}
