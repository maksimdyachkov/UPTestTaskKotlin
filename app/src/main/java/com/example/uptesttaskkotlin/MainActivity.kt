package com.example.uptesttaskkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uptesttaskkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var cameraController: BarcodeCameraController

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        cameraController = BarcodeCameraController(this, this, binding.previewView)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        val adapter = BarcodeAdapter()
        binding.rvScanHistory.layoutManager = LinearLayoutManager(this)
        binding.rvScanHistory.adapter = adapter
    }

    private fun setupObservers() {
        // Observe history to submit to adapter
        viewModel.scanHistory.observe(this) { history ->
            (binding.rvScanHistory.adapter as? BarcodeAdapter)?.submitList(history)
        }

        // Observe current result text
        viewModel.currentScanResult.observe(this) { result ->
            binding.tvScanResult.text = result
        }

        // Observe scanning state
        viewModel.isScanning.observe(this) { isScanning ->
            // A stopped PreviewView keeps showing its last frame, so hide it behind a hint.
            binding.previewView.isInvisible = !isScanning
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
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraController.start(BarcodeAnalyzer(viewModel::onBarcodeScanned))
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
}
