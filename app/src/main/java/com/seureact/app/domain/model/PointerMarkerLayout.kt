package com.seureact.app.domain.model

data class PointerDrawablePlacement(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val scaleX: Float,
    val scaleY: Float,
    val pivotFractionX: Float,
    val pivotFractionY: Float,
)

object PointerMarkerLayout {
    fun place(
        marker: PointerMarker,
        containerWidth: Float,
        containerHeight: Float,
        size: Float,
    ): PointerDrawablePlacement {
        val x = marker.x * containerWidth
        val y = marker.y * containerHeight
        val flipY = marker.y > 0.62f
        return when (marker.style) {
            PointerStyle.ARROW -> placementAtHotspot(
                x = x,
                y = y,
                size = size,
                hotspotX = 88f / 96f,
                hotspotY = 8f / 96f,
                scaleX = if (marker.x < 0.5f) -1f else 1f,
                scaleY = if (flipY) -1f else 1f,
            )
            PointerStyle.FINGER, PointerStyle.MOUSE -> placementAtHotspot(
                x = x,
                y = y,
                size = size,
                hotspotX = 0.5f,
                hotspotY = 0.5f,
                scaleX = 1f,
                scaleY = 1f,
            )
        }
    }

    private fun placementAtHotspot(
        x: Float,
        y: Float,
        size: Float,
        hotspotX: Float,
        hotspotY: Float,
        scaleX: Float,
        scaleY: Float,
    ) = PointerDrawablePlacement(
        left = x - size * hotspotX,
        top = y - size * hotspotY,
        right = x + size * (1f - hotspotX),
        bottom = y + size * (1f - hotspotY),
        scaleX = scaleX,
        scaleY = scaleY,
        pivotFractionX = hotspotX,
        pivotFractionY = hotspotY,
    )
}
