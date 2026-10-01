package com.example.uptesttaskkotlin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val repository = BarcodeRepository()

    val scanHistory: LiveData<List<BarcodeItem>> = repository.history

    private val _currentScanResult = MutableLiveData<String>("No barcode scanned yet")
    val currentScanResult: LiveData<String> = _currentScanResult

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    fun toggleScanning() {
        _isScanning.value = !(_isScanning.value ?: false)
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    fun onBarcodeScanned(displayValue: String, rawValue: String?) {
        _currentScanResult.value = displayValue
        repository.addBarcode(displayValue, rawValue)
    }

    fun clearHistory() {
        repository.clearHistory()
        _currentScanResult.value = "No barcode scanned yet"
    }
}
