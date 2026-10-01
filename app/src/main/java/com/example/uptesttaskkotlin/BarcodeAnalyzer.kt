package com.example.uptesttaskkotlin

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable
import java.util.concurrent.Executor

/** A barcode found in a camera frame and its position within the visible preview area. */
data class DetectedBarcode(
    val displayValue: String,
    val rawValue: String?,
    val bounds: NormalizedRect
)

/**
 * [analyze] is called by CameraX on a background thread. Results are delivered on
 * [callbackExecutor], so the caller decides which thread [onBarcodesDetected] runs on.
 *
 * Holds an ML Kit scanner with native resources: call [close] when the analyzer is
 * no longer needed.
 */
class BarcodeAnalyzer(
    private val callbackExecutor: Executor,
    private val onBarcodesDetected: (List<DetectedBarcode>) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            // ML Kit scans the whole buffer, the preview shows only its crop rect.
            val visibleArea = imageProxy.cropRect.toPixelRect()
                .toUpright(imageProxy.width, imageProxy.height, rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener(callbackExecutor) { barcodes ->
                    val detected = barcodes.mapNotNull { it.toDetectedBarcode(visibleArea) }
                    if (detected.isNotEmpty()) {
                        onBarcodesDetected(detected)
                    }
                }
                .addOnFailureListener(callbackExecutor) {
                    // Failures can be handled here
                }
                .addOnCompleteListener(callbackExecutor) {
                    // ML Kit reads the frame asynchronously, so the proxy can be released only
                    // once processing is done. Until then CameraX delivers no new frames.
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun close() {
        scanner.close()
    }

    private fun Barcode.toDetectedBarcode(visibleArea: PixelRect): DetectedBarcode? {
        val value = displayValue ?: rawValue
        val box = boundingBox
        if (value.isNullOrEmpty() || box == null) return null
        return DetectedBarcode(value, rawValue, NormalizedRect.of(box.toPixelRect(), visibleArea))
    }

    private fun Rect.toPixelRect() = PixelRect(left, top, right, bottom)
}
