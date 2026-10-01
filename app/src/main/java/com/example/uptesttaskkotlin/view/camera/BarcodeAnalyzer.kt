package com.example.uptesttaskkotlin.view.camera

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.Closeable
import java.util.concurrent.Executor

/** A barcode found in a camera frame; [bounds] are in the coordinates of the PreviewView. */
data class DetectedBarcode(
    val displayValue: String,
    val rawValue: String?,
    val bounds: Rect
)

/**
 * Detects barcodes with ML Kit and reports them on [callbackExecutor].
 *
 * Frame handling is delegated to CameraX [MlKitAnalyzer]: it feeds frames to the scanner,
 * releases every ImageProxy once ML Kit is done with it and converts barcode positions
 * to PreviewView coordinates.
 *
 * Holds an ML Kit scanner with native resources: call [close] when it is no longer needed.
 */
class BarcodeAnalyzer(
    callbackExecutor: Executor,
    private val onBarcodesDetected: (List<DetectedBarcode>) -> Unit
) : Closeable {

    private val scanner = BarcodeScanning.getClient()

    val analyzer: ImageAnalysis.Analyzer = MlKitAnalyzer(
        listOf(scanner),
        COORDINATE_SYSTEM_VIEW_REFERENCED,
        callbackExecutor
    ) { result ->
        val detected = result.getValue(scanner).orEmpty().mapNotNull { it.toDetectedBarcode() }
        if (detected.isNotEmpty()) {
            onBarcodesDetected(detected)
        }
    }

    override fun close() {
        scanner.close()
    }

    private fun Barcode.toDetectedBarcode(): DetectedBarcode? {
        val value = displayValue ?: rawValue
        val box = boundingBox
        if (value.isNullOrEmpty() || box == null) return null
        return DetectedBarcode(value, rawValue, box)
    }
}
