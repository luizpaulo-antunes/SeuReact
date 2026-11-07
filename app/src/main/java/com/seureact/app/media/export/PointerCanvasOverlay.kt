package com.seureact.app.media.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import com.seureact.app.domain.model.PointerMarker
import com.seureact.app.domain.model.PointerStyle
import kotlin.math.PI
import kotlin.math.sin

@UnstableApi
internal class PointerCanvasOverlay(private val marker: PointerMarker) : CanvasOverlay(true) {
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 99, 235)
        style = Paint.Style.FILL
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val navy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 23, 42)
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val x = marker.x * canvas.width
        val y = marker.y * canvas.height
        val size = (minOf(canvas.width, canvas.height) * 0.07f).coerceIn(42f, 112f)
        drawClickTarget(canvas, x, y, size, presentationTimeUs)
        when (marker.style) {
            PointerStyle.FINGER -> drawFinger(canvas, x, y, size)
            PointerStyle.ARROW -> drawArrow(canvas, x, y, size)
            PointerStyle.MOUSE -> drawMouse(canvas, x, y, size)
        }
    }

    private fun drawClickTarget(canvas: Canvas, x: Float, y: Float, size: Float, presentationTimeUs: Long) {
        val phase = (presentationTimeUs % 600_000L) / 600_000f
        val pulse = sin(phase * PI).toFloat().coerceAtLeast(0f)
        canvas.drawCircle(x, y, size * (0.58f + 0.34f * pulse), Paint(blue).apply { alpha = (48 * (1f - phase)).toInt() })
        canvas.drawCircle(x, y, size * 0.44f, Paint(blue).apply { alpha = 82 })
        canvas.drawCircle(x, y, size * 0.16f, white)
    }

    private fun drawFinger(canvas: Canvas, x: Float, y: Float, size: Float) {
        val palm = RectF(x - size * 0.48f, y + size * 0.48f, x + size * 0.48f, y + size * 1.18f)
        canvas.drawRoundRect(palm, size * 0.28f, size * 0.28f, white)
        canvas.drawRoundRect(
            RectF(x - size * 0.14f, y + size * 0.08f, x + size * 0.14f, y + size * 0.88f),
            size * 0.14f,
            size * 0.14f,
            white,
        )
        canvas.drawCircle(x - size * 0.28f, y + size * 0.58f, size * 0.18f, white)
        canvas.drawCircle(x + size * 0.28f, y + size * 0.58f, size * 0.18f, white)
        canvas.drawRoundRect(
            RectF(x - size * 0.65f, y + size * 0.68f, x - size * 0.18f, y + size * 0.98f),
            size * 0.15f,
            size * 0.15f,
            white,
        )
        canvas.drawCircle(x, y + size * 0.2f, size * 0.08f, blue)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, size: Float) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x - size * 0.2f, y + size * 1.12f)
            lineTo(x + size * 0.12f, y + size * 0.86f)
            lineTo(x + size * 0.42f, y + size * 1.38f)
            lineTo(x + size * 0.68f, y + size * 1.22f)
            lineTo(x + size * 0.38f, y + size * 0.72f)
            lineTo(x + size * 0.78f, y + size * 0.66f)
            close()
        }
        canvas.drawPath(path, white)
        canvas.drawCircle(x + size * 0.18f, y + size * 0.72f, size * 0.08f, blue)
    }

    private fun drawMouse(canvas: Canvas, x: Float, y: Float, size: Float) {
        val body = RectF(x - size * 0.48f, y + size * 0.18f, x + size * 0.48f, y + size * 1.38f)
        canvas.drawRoundRect(body, size * 0.46f, size * 0.46f, white)
        canvas.drawRoundRect(
            RectF(x - size * 0.09f, y + size * 0.36f, x + size * 0.09f, y + size * 0.72f),
            size * 0.09f,
            size * 0.09f,
            blue,
        )
        canvas.drawRect(x - size * 0.025f, y + size * 0.2f, x + size * 0.025f, y + size * 0.94f, navy)
    }
}
