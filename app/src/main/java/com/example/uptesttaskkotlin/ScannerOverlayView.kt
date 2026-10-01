package com.example.uptesttaskkotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Viewfinder drawn over the camera preview: dims everything except a centered square
 * and marks its corners. The corners stay highlighted while [showDetected] keeps being called,
 * i.e. while a barcode is in view.
 *
 * The view must cover the camera preview exactly: [isInsideFrame] relies on it to tell
 * whether a detected barcode lies within the frame.
 */
class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val cornerRadius = resources.getDimension(R.dimen.scanner_frame_corner_radius)
    private val cornerLength = resources.getDimension(R.dimen.scanner_frame_corner_length)
    private val frameTolerance = resources.getDimension(R.dimen.scanner_frame_tolerance)
    private val idleColor = ContextCompat.getColor(context, R.color.scanner_frame)
    private val detectedColor = ContextCompat.getColor(context, R.color.scanner_frame_detected)

    private val scrimPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.scanner_scrim)
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = resources.getDimension(R.dimen.scanner_frame_stroke_width)
        color = idleColor
    }

    private val frame = RectF()
    private val arcBounds = RectF()
    private val scrimPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
    private val cornersPath = Path()
    private val resetCornerColor = Runnable { setCornerColor(idleColor) }

    /** Highlights the corners until no detection has been reported for [DETECTED_HOLD_MS]. */
    fun showDetected() {
        setCornerColor(detectedColor)
        removeCallbacks(resetCornerColor)
        postDelayed(resetCornerColor, DETECTED_HOLD_MS)
    }

    /**
     * Whether [bounds], given in the coordinates of this view, fit into the frame.
     * A small overshoot at the edges is allowed.
     */
    fun isInsideFrame(bounds: Rect): Boolean =
        bounds.left >= frame.left - frameTolerance &&
            bounds.top >= frame.top - frameTolerance &&
            bounds.right <= frame.right + frameTolerance &&
            bounds.bottom <= frame.bottom + frameTolerance

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val side = min(w, h) * FRAME_SIZE_RATIO
        frame.set((w - side) / 2f, (h - side) / 2f, (w + side) / 2f, (h + side) / 2f)

        // EVEN_ODD turns the inner rectangle into a hole in the scrim.
        scrimPath.reset()
        scrimPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        scrimPath.addRoundRect(frame, cornerRadius, cornerRadius, Path.Direction.CW)

        cornersPath.reset()
        addCorner(frame.left, frame.top + cornerLength, frame.left, frame.top, 180f,
            frame.left + cornerLength, frame.top)
        addCorner(frame.right - cornerLength, frame.top, frame.right, frame.top, 270f,
            frame.right, frame.top + cornerLength)
        addCorner(frame.right, frame.bottom - cornerLength, frame.right, frame.bottom, 0f,
            frame.right - cornerLength, frame.bottom)
        addCorner(frame.left + cornerLength, frame.bottom, frame.left, frame.bottom, 90f,
            frame.left, frame.bottom - cornerLength)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(scrimPath, scrimPaint)
        canvas.drawPath(cornersPath, cornerPaint)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(resetCornerColor)
        super.onDetachedFromWindow()
    }

    /** Adds an L-shaped mark with a rounded bend at the frame vertex ([vertexX], [vertexY]). */
    private fun addCorner(
        startX: Float, startY: Float,
        vertexX: Float, vertexY: Float,
        startAngle: Float,
        endX: Float, endY: Float
    ) {
        val diameter = cornerRadius * 2
        val arcLeft = if (vertexX == frame.left) vertexX else vertexX - diameter
        val arcTop = if (vertexY == frame.top) vertexY else vertexY - diameter
        arcBounds.set(arcLeft, arcTop, arcLeft + diameter, arcTop + diameter)

        cornersPath.moveTo(startX, startY)
        cornersPath.arcTo(arcBounds, startAngle, 90f)
        cornersPath.lineTo(endX, endY)
    }

    private fun setCornerColor(color: Int) {
        if (cornerPaint.color == color) return
        cornerPaint.color = color
        invalidate()
    }

    private companion object {
        const val FRAME_SIZE_RATIO = 0.75f
        const val DETECTED_HOLD_MS = 500L
    }
}
