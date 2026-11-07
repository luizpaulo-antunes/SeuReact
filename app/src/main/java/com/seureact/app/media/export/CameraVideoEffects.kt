package com.seureact.app.media.export

import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import com.seureact.app.domain.model.CameraShape

internal fun cameraCropAspectRatio(shape: CameraShape): Float = when (shape) {
    CameraShape.CIRCLE, CameraShape.SQUARE -> 1f
    CameraShape.ROUNDED_RECTANGLE -> 4f / 3f
}

@UnstableApi
internal fun cameraVideoEffects(shape: CameraShape): List<Effect> = listOf(CameraMaskEffect(shape))
