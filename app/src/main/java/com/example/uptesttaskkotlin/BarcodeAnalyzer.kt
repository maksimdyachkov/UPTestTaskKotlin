package com.example.uptesttaskkotlin

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable
import java.util.concurrent.Executor

/**
 * [analyze] is called by CameraX on a background thread. Results are delivered on
 * [callbackExecutor], so the caller decides which thread [onBarcodeDetected] runs on.
 *
 * Holds an ML Kit scanner with native resources: call [close] when the analyzer is
 * no longer needed.
 */
class BarcodeAnalyzer(
    private val callbackExecutor: Executor,
    private val onBarcodeDetected: (displayValue: String, rawValue: String?) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener(callbackExecutor) { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.first()
                        val displayValue = firstBarcode.displayValue ?: firstBarcode.rawValue ?: ""
                        if (displayValue.isNotEmpty()) {
                            onBarcodeDetected(displayValue, firstBarcode.rawValue)
                        }
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
}
