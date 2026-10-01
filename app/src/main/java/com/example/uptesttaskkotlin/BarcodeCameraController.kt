package com.example.uptesttaskkotlin

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Binds CameraX [Preview] and [ImageAnalysis] use cases to [lifecycleOwner].
 *
 * Because the use cases are lifecycle-bound, CameraX pauses the camera automatically
 * when the app goes to background and resumes it on return. [stop] unbinds them
 * explicitly when scanning is turned off; the analysis executor is shut down
 * when [lifecycleOwner] is destroyed.
 */
class BarcodeCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) : DefaultLifecycleObserver {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isStartRequested = false

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun start(analyzer: ImageAnalysis.Analyzer) {
        isStartRequested = true
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            // The view port is derived from the PreviewView size, so wait until it is laid out.
            previewView.doOnLayout {
                // Scanning may have been stopped while the provider was initializing.
                if (!isStartRequested) return@doOnLayout
                try {
                    val provider = providerFuture.get().also { cameraProvider = it }
                    bindUseCases(provider, analyzer)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start camera", e)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        isStartRequested = false
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        cameraProvider?.unbindAll()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        analysisExecutor.shutdown()
        owner.lifecycle.removeObserver(this)
    }

    private fun bindUseCases(provider: ProcessCameraProvider, analyzer: ImageAnalysis.Analyzer) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            // Drop stale frames instead of queueing them while ML Kit is busy.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        val useCases = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(analysis)
        // Makes ImageProxy.cropRect match the part of the frame that the preview shows,
        // so barcode positions can be compared with the viewfinder on screen.
        previewView.viewPort?.let(useCases::setViewPort)

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCases.build())
        imageAnalysis = analysis
    }

    private companion object {
        const val TAG = "BarcodeCameraController"
    }
}
