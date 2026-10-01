package com.example.uptesttaskkotlin

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (displayValue: String, rawValue: String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.first()
                        val displayValue = firstBarcode.displayValue ?: firstBarcode.rawValue ?: ""
                        if (displayValue.isNotEmpty()) {
                            onBarcodeDetected(displayValue, firstBarcode.rawValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Failures can be handled here
                }
                .addOnCompleteListener {
                    // ML Kit reads the frame asynchronously, so the proxy can be released only
                    // once processing is done. Until then CameraX delivers no new frames.
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
