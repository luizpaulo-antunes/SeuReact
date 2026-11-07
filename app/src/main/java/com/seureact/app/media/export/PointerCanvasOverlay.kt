package com.seureact.app.media.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.graphics.withScale
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import com.seureact.app.domain.model.PointerMarker
import com.seureact.app.domain.model.PointerMarkerLayout
import com.seureact.app.media.PointerMarkerVisuals
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@UnstableApi
internal class PointerCanvasOverlay(context: Context, private val marker: PointerMarker) : CanvasOverlay(true) {
    private val drawable = checkNotNull(ContextCompat.getDrawable(context, PointerMarkerVisuals.drawable(marker.style))).mutate()
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 99, 235)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val x = marker.x * canvas.width
        val y = marker.y * canvas.height
        val size = (minOf(canvas.width, canvas.height) * 0.13f).coerceIn(64f, 180f)
        drawPulse(canvas, x, y, size, presentationTimeUs)
        val placement = PointerMarkerLayout.place(marker, canvas.width.toFloat(), canvas.height.toFloat(), size)
        drawable.bounds = Rect(
            placement.left.roundToInt(),
            placement.top.roundToInt(),
            placement.right.roundToInt(),
            placement.bottom.roundToInt(),
        )
        canvas.withScale(placement.scaleX, placement.scaleY, x, y) {
            drawable.draw(this)
        }
    }

    private fun drawPulse(canvas: Canvas, x: Float, y: Float, size: Float, presentationTimeUs: Long) {
        val phase = (presentationTimeUs % 600_000L) / 600_000f
        val pulse = sin(phase * PI).toFloat().coerceAtLeast(0f)
        canvas.drawCircle(x, y, size * (0.1f + 0.09f * pulse), Paint(pulsePaint).apply {
            alpha = (90 * (1f - phase)).toInt()
        })
    }
}
