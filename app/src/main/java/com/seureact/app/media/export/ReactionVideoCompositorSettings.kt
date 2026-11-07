package com.seureact.app.media.export

import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.effect.StaticOverlaySettings
import com.seureact.app.domain.model.OutputAspect
import com.seureact.app.domain.model.OutputResolution
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.timeline.RenderSegment

@UnstableApi
class ReactionVideoCompositorSettings(
    private val project: ReactionProject,
    private val segments: List<RenderSegment>,
) : VideoCompositorSettings {
    override fun getOutputSize(inputSizes: List<Size>): Size {
        require(inputSizes.size == ReactionVideoLayerOrder.compositionOrder.size) {
            "A composição requer as camadas de câmera e vídeo original"
        }
        val longEdge = if (project.outputResolution == OutputResolution.FULL_HD_1080) 1920 else 1280
        return when (project.outputAspect) {
            OutputAspect.PORTRAIT_9_16 -> Size(longEdge * 9 / 16, longEdge)
            OutputAspect.LANDSCAPE_16_9 -> Size(longEdge, longEdge * 9 / 16)
            OutputAspect.SQUARE -> Size(if (longEdge == 1920) 1080 else 720, if (longEdge == 1920) 1080 else 720)
            OutputAspect.ORIGINAL -> {
                val scale = longEdge.toFloat() / maxOf(project.metadata.width, project.metadata.height)
                Size(even(project.metadata.width * scale), even(project.metadata.height * scale))
            }
        }
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        if (ReactionVideoLayerOrder.layerForInputId(inputId) != ReactionVideoLayer.CAMERA) {
            return StaticOverlaySettings.Builder().build()
        }
        val timeNanos = presentationTimeUs * 1_000L
        val transform = segments.firstOrNull { timeNanos in it.finalStartNanos until it.finalEndNanos }
            ?.cameraTransform ?: project.cameraTransform
        val scaleX = if (transform.mirrored) -transform.width else transform.width
        return StaticOverlaySettings.Builder()
            .setScale(scaleX, transform.height)
            .setOverlayFrameAnchor(0f, 0f)
            .setBackgroundFrameAnchor(transform.centerX * 2f - 1f, 1f - transform.centerY * 2f)
            .build()
    }

    private fun even(value: Float): Int = (value.toInt().coerceAtLeast(2) / 2) * 2
}
