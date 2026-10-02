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

    /** The barcode scanned last, or null if nothing has been scanned yet. */
    private val _currentScanResult = MutableLiveData<String?>(null)
    val currentScanResult: LiveData<String?> = _currentScanResult

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

    /** When each barcode value was last detected; holds only values seen recently. */
    private val lastSeenAt = mutableMapOf<String, Long>()

    fun toggleScanning() {
        _isScanning.value = !(_isScanning.value ?: false)
    }

    /**
     * The analyzer reports a barcode for every frame it is visible in. A code is treated as
     * a new scan only if it has been out of sight for at least [DUPLICATE_WINDOW_MS].
     * Every value is tracked separately, so several codes in view do not re-add each other.
     */
    @MainThread
    fun onBarcodeScanned(displayValue: String, rawValue: String?) {
        // A frame that was still being processed when scanning stopped reports its result late.
        if (_isScanning.value != true) return

        val now = clock.now()
        lastSeenAt.values.removeAll { seenAt -> now - seenAt >= DUPLICATE_WINDOW_MS }
        val isRepeat = lastSeenAt.put(displayValue, now) != null
        if (isRepeat) return

        _currentScanResult.value = displayValue
        viewModelScope.launch { repository.addBarcode(displayValue, rawValue) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
        lastSeenAt.clear()
        _currentScanResult.value = null
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 2_000L
    }
}
