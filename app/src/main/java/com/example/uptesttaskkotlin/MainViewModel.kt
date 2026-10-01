package com.example.uptesttaskkotlin

import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel(
    private val clock: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {

    private val repository = BarcodeRepository()

    val scanHistory: LiveData<List<BarcodeItem>> = repository.history

    private val _currentScanResult = MutableLiveData<String>("No barcode scanned yet")
    val currentScanResult: LiveData<String> = _currentScanResult

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private var lastScannedValue: String? = null
    private var lastSeenAt = 0L

    fun toggleScanning() {
        _isScanning.value = !(_isScanning.value ?: false)
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    /**
     * The analyzer reports a barcode for every frame it is visible in. A code is treated as
     * a new scan only if it differs from the previous one or was out of sight for at least
     * [DUPLICATE_WINDOW_MS].
     */
    @MainThread
    fun onBarcodeScanned(displayValue: String, rawValue: String?) {
        val now = clock()
        val isRepeat = displayValue == lastScannedValue && now - lastSeenAt < DUPLICATE_WINDOW_MS
        lastScannedValue = displayValue
        lastSeenAt = now
        if (isRepeat) return

        _currentScanResult.value = displayValue
        repository.addBarcode(displayValue, rawValue)
    }

    fun clearHistory() {
        repository.clearHistory()
        lastScannedValue = null
        _currentScanResult.value = "No barcode scanned yet"
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 2_000L
    }
}
