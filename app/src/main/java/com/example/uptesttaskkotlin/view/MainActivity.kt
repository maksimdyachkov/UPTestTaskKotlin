package com.example.uptesttaskkotlin.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptesttaskkotlin.R
import com.example.uptesttaskkotlin.databinding.ActivityMainBinding
import com.example.uptesttaskkotlin.view.camera.BarcodeAnalyzer
import com.example.uptesttaskkotlin.view.camera.BarcodeCameraController
import com.example.uptesttaskkotlin.view.camera.DetectedBarcode
import com.example.uptesttaskkotlin.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraController: BarcodeCameraController
    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private val historyAdapter = BarcodeAdapter()

    // Survives rotation, so the system dialog that is already on screen is not requested again.
    private var isPermissionRequestPending = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionRequestPending = false
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
            viewModel.stopScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isPermissionRequestPending =
            savedInstanceState?.getBoolean(KEY_PERMISSION_REQUEST_PENDING) ?: false

        cameraController = BarcodeCameraController(this, this, binding.previewView)
        barcodeAnalyzer = BarcodeAnalyzer(
            callbackExecutor = ContextCompat.getMainExecutor(this),
            onBarcodesDetected = ::onBarcodesDetected
        )

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvScanHistory.layoutManager = LinearLayoutManager(this)
        binding.rvScanHistory.adapter = historyAdapter
    }

    private fun setupObservers() {
        // Observe history to submit to adapter
        viewModel.scanHistory.observe(this) { history ->
            val previousFirst = historyAdapter.currentList.firstOrNull()
            historyAdapter.submitList(history) {
                // RecyclerView keeps its position when an item is inserted above the visible
                // ones, which would leave a new scan off-screen in a long history.
                if (previousFirst != null && history.firstOrNull() != previousFirst) {
                    binding.rvScanHistory.scrollToPosition(0)
                }
            }
        }

        // Observe current result text
        viewModel.currentScanResult.observe(this) { result ->
            binding.tvScanResult.text = if (result == null) {
                getString(R.string.no_barcode_scanned)
            } else {
                getString(R.string.scanned_result_prefix, result)
            }
        }

        viewModel.barcodeInViewEvent.observe(this) { event ->
            event.getContentIfNotHandled()?.let { binding.scannerOverlay.showDetected() }
        }

        // Observe scanning state
        viewModel.isScanning.observe(this) { isScanning ->
            // A stopped PreviewView keeps showing its last frame, so hide it behind a hint.
            binding.previewView.isInvisible = !isScanning
            binding.scannerOverlay.isInvisible = !isScanning
            binding.tvCameraHint.isVisible = !isScanning
            if (isScanning) {
                binding.btnToggleScan.text = getString(R.string.stop_scanning)
                checkPermissionsAndStartCamera()
            } else {
                binding.btnToggleScan.text = getString(R.string.start_scanning)
                cameraController.stop()
            }
        }
    }

    private fun setupListeners() {
        binding.btnToggleScan.setOnClickListener {
            viewModel.toggleScanning()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun checkPermissionsAndStartCamera() {
        if (isCameraPermissionGranted()) {
            startCamera()
        } else if (!isPermissionRequestPending) {
            isPermissionRequestPending = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraController.start(barcodeAnalyzer.analyzer)
    }

    private fun onBarcodesDetected(barcodes: List<DetectedBarcode>) {
        // Only barcodes that fit into the viewfinder count as scanned.
        barcodes
            .filter { binding.scannerOverlay.isInsideFrame(it.bounds) }
            .forEach { viewModel.onBarcodeScanned(it.displayValue, it.rawValue) }
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PERMISSION_REQUEST_PENDING, isPermissionRequestPending)
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeAnalyzer.close()
    }

    private companion object {
        const val KEY_PERMISSION_REQUEST_PENDING = "permission_request_pending"
    }
}
