package com.example.uptesttaskkotlin.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.uptesttaskkotlin.model.BarcodeItem
import com.example.uptesttaskkotlin.model.repository.BarcodeRepository
import com.example.uptesttaskkotlin.util.ElapsedClock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    private val clock: ElapsedClock
) : ViewModel() {

    val scanHistory: LiveData<List<BarcodeItem>> = repository.history.asLiveData()

    private val _currentScanResult = MutableLiveData<String>("No barcode scanned yet")
    val currentScanResult: LiveData<String> = _currentScanResult

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    /** Emitted for every frame a barcode is detected in, including repeats of the same code. */
    private val _barcodeInViewEvent = MutableLiveData<Event<Unit>>()
    val barcodeInViewEvent: LiveData<Event<Unit>> = _barcodeInViewEvent

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
        _barcodeInViewEvent.value = Event(Unit)

        val now = clock.now()
        val isRepeat = displayValue == lastScannedValue && now - lastSeenAt < DUPLICATE_WINDOW_MS
        lastScannedValue = displayValue
        lastSeenAt = now
        if (isRepeat) return

        _currentScanResult.value = displayValue
        viewModelScope.launch { repository.addBarcode(displayValue, rawValue) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
        lastScannedValue = null
        _currentScanResult.value = "No barcode scanned yet"
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 2_000L
    }
}
