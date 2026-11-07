package com.seureact.app.media

import androidx.annotation.DrawableRes
import com.seureact.app.R
import com.seureact.app.domain.model.PointerStyle

object PointerMarkerVisuals {
    @DrawableRes
    fun drawable(style: PointerStyle): Int = when (style) {
        PointerStyle.FINGER -> R.drawable.ic_marker_finger
        PointerStyle.ARROW -> R.drawable.ic_marker_arrow
        PointerStyle.MOUSE -> R.drawable.ic_marker_mouse
    }
}
