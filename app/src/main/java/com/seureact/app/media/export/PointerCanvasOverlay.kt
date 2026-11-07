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

@UnstableApi
internal class PointerCanvasOverlay(private val marker: PointerMarker) : CanvasOverlay(true) {
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 214, 10)
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 3f, Color.argb(150, 0, 0, 0))
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        val x = marker.x * canvas.width
        val y = marker.y * canvas.height
        val size = minOf(canvas.width, canvas.height) * 0.075f
        canvas.drawCircle(x, y, size * 0.9f, Paint(accent).apply { alpha = 70 })
        when (marker.style) {
            PointerStyle.FINGER -> drawFinger(canvas, x, y, size)
            PointerStyle.ARROW -> drawArrow(canvas, x, y, size)
            PointerStyle.MOUSE -> drawMouse(canvas, x, y, size)
        }
    }

    private fun drawFinger(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawCircle(x, y, size * 0.28f, accent)
        canvas.drawLine(x, y + size * 0.15f, x, y + size, outline)
        canvas.drawCircle(x, y, size * 0.28f, outline)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, size: Float) {
        val path = Path().apply {
            moveTo(x - size, y + size)
            lineTo(x + size * 0.25f, y - size * 0.25f)
            lineTo(x - size * 0.15f, y - size * 0.35f)
            lineTo(x + size, y - size)
            lineTo(x + size * 0.35f, y + size * 0.15f)
            lineTo(x + size * 0.25f, y - size * 0.25f)
            close()
        }
        canvas.drawPath(path, accent)
        canvas.drawPath(path, outline)
    }

    private fun drawMouse(canvas: Canvas, x: Float, y: Float, size: Float) {
        val body = RectF(x - size * 0.55f, y - size, x + size * 0.55f, y + size)
        canvas.drawRoundRect(body, size * 0.5f, size * 0.5f, accent)
        canvas.drawRoundRect(body, size * 0.5f, size * 0.5f, outline)
        canvas.drawLine(x, y - size, x, y - size * 0.25f, outline)
    }
}
