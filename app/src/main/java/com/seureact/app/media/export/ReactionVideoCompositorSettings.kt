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

internal data class CameraOverlayScale(val x: Float, val y: Float)

@UnstableApi
internal fun cameraOverlayScale(
    input: Size,
    output: Size,
    transform: com.seureact.app.domain.model.CameraTransform,
    shape: com.seureact.app.domain.model.CameraShape,
): CameraOverlayScale {
    val targetWidth = transform.width * output.width
    val targetHeight = when (shape) {
        com.seureact.app.domain.model.CameraShape.CIRCLE,
        com.seureact.app.domain.model.CameraShape.SQUARE -> targetWidth
        com.seureact.app.domain.model.CameraShape.ROUNDED_RECTANGLE -> targetWidth * 3f / 4f
    }
    return CameraOverlayScale(
        x = targetWidth / input.width * if (transform.mirrored) -1f else 1f,
        y = targetHeight / input.height,
    )
}

@UnstableApi
class ReactionVideoCompositorSettings(
    private val project: ReactionProject,
    private val segments: List<RenderSegment>,
    private val sourceDisplaySize: Size = Size(project.metadata.width, project.metadata.height),
    private val cameraInputSize: Size,
) : VideoCompositorSettings {
    private val outputSize = calculateOutputSize()

    override fun getOutputSize(inputSizes: List<Size>): Size {
        require(inputSizes.size == ReactionVideoLayerOrder.compositionOrder.size) {
            "A composição requer as camadas de câmera e vídeo original"
        }
        return outputSize
    }

    private fun calculateOutputSize(): Size {
        val longEdge = if (project.outputResolution == OutputResolution.FULL_HD_1080) 1920 else 1280
        return when (project.outputAspect) {
            OutputAspect.PORTRAIT_9_16 -> Size(longEdge * 9 / 16, longEdge)
            OutputAspect.LANDSCAPE_16_9 -> Size(longEdge, longEdge * 9 / 16)
            OutputAspect.SQUARE -> Size(if (longEdge == 1920) 1080 else 720, if (longEdge == 1920) 1080 else 720)
            OutputAspect.ORIGINAL -> {
                val scale = longEdge.toFloat() / maxOf(sourceDisplaySize.width, sourceDisplaySize.height)
                Size(even(sourceDisplaySize.width * scale), even(sourceDisplaySize.height * scale))
            }
        }
    }

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        val timeNanos = presentationTimeUs * 1_000L
        val segment = segments.firstOrNull { timeNanos in it.finalStartNanos until it.finalEndNanos }
        return when (ReactionVideoLayerOrder.layerForInputId(inputId)) {
            ReactionVideoLayer.CAMERA -> {
                val transform = segment?.cameraTransform ?: project.cameraTransform
                val scale = cameraScaleAt(presentationTimeUs)
                StaticOverlaySettings.Builder()
                    .setScale(scale.x, scale.y)
                    .setOverlayFrameAnchor(0f, 0f)
                    .setBackgroundFrameAnchor(transform.centerX * 2f - 1f, 1f - transform.centerY * 2f)
                    .build()
            }
            ReactionVideoLayer.SOURCE -> {
                val transform = segment?.sourceTransform ?: com.seureact.app.domain.model.SourceTransform()
                val anchorX = transform.focusX * 2f - 1f
                val anchorY = 1f - transform.focusY * 2f
                StaticOverlaySettings.Builder()
                    .setScale(transform.scale, transform.scale)
                    .setOverlayFrameAnchor(anchorX, anchorY)
                    .setBackgroundFrameAnchor(anchorX, anchorY)
                    .build()
            }
            null -> StaticOverlaySettings.Builder().build()
        }
    }

    internal fun cameraScaleAt(presentationTimeUs: Long): CameraOverlayScale {
        val timeNanos = presentationTimeUs * 1_000L
        val segment = segments.firstOrNull { timeNanos in it.finalStartNanos until it.finalEndNanos }
        return cameraOverlayScale(
            cameraInputSize,
            outputSize,
            segment?.cameraTransform ?: project.cameraTransform,
            segment?.cameraShape ?: project.cameraShape,
        )
    }

    private fun even(value: Float): Int = (value.toInt().coerceAtLeast(2) / 2) * 2
}
