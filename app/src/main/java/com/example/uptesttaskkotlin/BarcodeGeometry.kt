package com.example.uptesttaskkotlin

/** A rectangle in image pixels. */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val width: Int get() = right - left
    val height: Int get() = bottom - top

    /**
     * Maps this rectangle from a [bufferWidth] x [bufferHeight] camera buffer to the upright
     * image obtained by rotating that buffer clockwise by [rotationDegrees].
     */
    fun toUpright(bufferWidth: Int, bufferHeight: Int, rotationDegrees: Int): PixelRect =
        when (rotationDegrees) {
            90 -> PixelRect(bufferHeight - bottom, left, bufferHeight - top, right)
            180 -> PixelRect(bufferWidth - right, bufferHeight - bottom, bufferWidth - left, bufferHeight - top)
            270 -> PixelRect(top, bufferWidth - right, bottom, bufferWidth - left)
            else -> this
        }
}

/**
 * A rectangle expressed as fractions of the camera image area that is visible in the preview:
 * (0, 0) is its top-left corner and (1, 1) its bottom-right one. Values outside 0..1 mean
 * the rectangle reaches beyond the visible area.
 */
data class NormalizedRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    companion object {
        /**
         * @param box bounds in the upright image, as reported by ML Kit
         * @param visibleArea part of the upright image that is shown in the preview
         */
        fun of(box: PixelRect, visibleArea: PixelRect): NormalizedRect {
            val width = visibleArea.width.toFloat()
            val height = visibleArea.height.toFloat()
            return NormalizedRect(
                left = (box.left - visibleArea.left) / width,
                top = (box.top - visibleArea.top) / height,
                right = (box.right - visibleArea.left) / width,
                bottom = (box.bottom - visibleArea.top) / height
            )
        }
    }
}
