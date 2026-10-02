package com.example.uptesttaskkotlin.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraController: BarcodeCameraController
    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private val historyAdapter = BarcodeAdapter()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleScanning()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Observe scanning state
        viewModel.isScanning.observe(this) { isScanning ->
            // A stopped PreviewView keeps showing its last frame, so hide it behind a hint.
            binding.previewView.isInvisible = !isScanning
            binding.scannerOverlay.isInvisible = !isScanning
            binding.tvCameraHint.isVisible = !isScanning
            if (isScanning) {
                binding.btnToggleScan.text = getString(R.string.stop_scanning)
                cameraController.start(barcodeAnalyzer.analyzer)
            } else {
                binding.btnToggleScan.text = getString(R.string.start_scanning)
                cameraController.stop()
            }
        }
    }

    private fun setupListeners() {
        binding.btnToggleScan.setOnClickListener {
            // Scanning is turned on only once the camera permission is granted, so the
            // scanning state never needs a permission check of its own.
            val isScanning = viewModel.isScanning.value == true
            if (isScanning || isCameraPermissionGranted()) {
                viewModel.toggleScanning()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun onBarcodesDetected(barcodes: List<DetectedBarcode>) {
        // Only barcodes that fit into the viewfinder count as scanned.
        val barcodesInFrame = barcodes.filter { binding.scannerOverlay.isInsideFrame(it.bounds) }
        if (barcodesInFrame.isEmpty()) return

        binding.scannerOverlay.showDetected()
        barcodesInFrame.forEach { viewModel.onBarcodeScanned(it.displayValue, it.rawValue) }
    }

    private fun showPermissionDenied() {
        // Once the system stops offering its dialog, every request is denied at once and
        // the permission can be granted only in the app settings.
        val canAskAgain = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        if (canAskAgain) {
            Snackbar.make(binding.root, R.string.camera_permission_denied, Snackbar.LENGTH_LONG)
                .show()
        } else {
            Snackbar.make(binding.root, R.string.camera_permission_blocked, Snackbar.LENGTH_LONG)
                .setAction(R.string.open_settings) { openAppSettings() }
                .show()
        }
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        barcodeAnalyzer.close()
    }
}
