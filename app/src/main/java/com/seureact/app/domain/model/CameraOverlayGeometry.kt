package com.seureact.app.domain.model

data class NormalizedOverlaySize(val width: Float, val height: Float)

object CameraOverlayGeometry {
    fun normalizedSize(
        transform: CameraTransform,
        shape: CameraShape,
        outputAspectRatio: Float,
    ): NormalizedOverlaySize {
        val physicalAspectRatio = when (shape) {
            CameraShape.CIRCLE, CameraShape.SQUARE -> 1f
            CameraShape.ROUNDED_RECTANGLE -> 4f / 3f
        }
        return NormalizedOverlaySize(
            width = transform.width,
            height = transform.width * outputAspectRatio / physicalAspectRatio,
        )
    }

    fun constrain(
        transform: CameraTransform,
        shape: CameraShape,
        outputAspectRatio: Float,
    ): CameraTransform {
        val width = transform.width.coerceIn(0.18f, 0.6f)
        val size = normalizedSize(transform.copy(width = width), shape, outputAspectRatio)
        val maximumHeight = size.height.coerceAtMost(0.92f)
        val adjustedWidth = if (size.height > maximumHeight) width * maximumHeight / size.height else width
        val adjustedSize = normalizedSize(transform.copy(width = adjustedWidth), shape, outputAspectRatio)
        return transform.copy(
            centerX = transform.centerX.coerceIn(adjustedWidth / 2f, 1f - adjustedWidth / 2f),
            centerY = transform.centerY.coerceIn(adjustedSize.height / 2f, 1f - adjustedSize.height / 2f),
            width = adjustedWidth,
            height = adjustedSize.height,
        )
    }

    fun outputAspectRatio(project: ReactionProject): Float = when (project.outputAspect) {
        OutputAspect.ORIGINAL -> project.metadata.width.toFloat() / project.metadata.height.coerceAtLeast(1)
        OutputAspect.PORTRAIT_9_16 -> 9f / 16f
        OutputAspect.LANDSCAPE_16_9 -> 16f / 9f
        OutputAspect.SQUARE -> 1f
    }
}
