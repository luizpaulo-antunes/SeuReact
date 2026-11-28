package com.seureact.app.domain.model

data class DualCameraPipTransform(
    val centerX: Float = 0.78f,
    val centerY: Float = 0.22f,
    val width: Float = 0.32f,
)

data class DualCameraCompositionTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float,
    val offsetY: Float,
)

object DualCameraGeometry {
    const val PORTRAIT_OUTPUT_ASPECT_RATIO = 9f / 16f

    fun pipHeight(transform: DualCameraPipTransform): Float = transform.width

    fun constrain(
        transform: DualCameraPipTransform,
    ): DualCameraPipTransform {
        val width = transform.width.coerceIn(MINIMUM_WIDTH, MAXIMUM_WIDTH)
        val height = pipHeight(transform.copy(width = width))
        return transform.copy(
            centerX = transform.centerX.coerceIn(width / 2f, 1f - width / 2f),
            centerY = transform.centerY.coerceIn(height / 2f, 1f - height / 2f),
            width = width,
        )
    }

    fun compositionTransform(
        transform: DualCameraPipTransform,
    ): DualCameraCompositionTransform {
        val constrained = constrain(transform)
        return DualCameraCompositionTransform(
            scaleX = constrained.width,
            scaleY = constrained.width,
            offsetX = constrained.centerX * 2f - 1f,
            offsetY = 1f - constrained.centerY * 2f,
        )
    }

    private const val MINIMUM_WIDTH = 0.22f
    private const val MAXIMUM_WIDTH = 0.48f
}
