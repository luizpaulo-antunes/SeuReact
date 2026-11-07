package com.seureact.app.media

data class VideoDimensions(val width: Int, val height: Int)

object SourceVideoGeometry {
    fun orientedSize(width: Int, height: Int, rotationDegrees: Int): VideoDimensions =
        if (rotationDegrees.mod(180) == 90) VideoDimensions(height, width) else VideoDimensions(width, height)
}
