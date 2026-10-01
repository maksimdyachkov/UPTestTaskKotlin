package com.example.uptesttaskkotlin.view.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Runs the camera preview and image analysis bound to [lifecycleOwner].
 *
 * Because the camera is lifecycle-bound, CameraX pauses it automatically when the app goes
 * to background and resumes it on return. [stop] unbinds it explicitly when scanning is
 * turned off; the analysis executor is shut down when [lifecycleOwner] is destroyed.
 */
class BarcodeCameraController(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) : DefaultLifecycleObserver {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val cameraController = LifecycleCameraController(context).apply {
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // Preview is always on; photo and video capture are not needed.
        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        // Drop stale frames instead of queueing them while ML Kit is busy.
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun start(analyzer: ImageAnalysis.Analyzer) {
        cameraController.setImageAnalysisAnalyzer(analysisExecutor, analyzer)
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }

    fun stop() {
        cameraController.clearImageAnalysisAnalyzer()
        cameraController.unbind()
        previewView.controller = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        analysisExecutor.shutdown()
        owner.lifecycle.removeObserver(this)
    }
}
