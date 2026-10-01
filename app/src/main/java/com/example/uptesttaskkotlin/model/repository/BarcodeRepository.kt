package com.example.uptesttaskkotlin.model.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.uptesttaskkotlin.model.BarcodeItem
import java.util.UUID

/**
 * A simple repository to manage scanned barcode history.
 * 
 * IMPROVEMENT: This could be backed by a Room database for persistence.
 */
class BarcodeRepository {
    private val _history = MutableLiveData<List<BarcodeItem>>(emptyList())
    val history: LiveData<List<BarcodeItem>> = _history

    fun addBarcode(value: String, rawValue: String?) {
        val newItem = BarcodeItem(
            id = UUID.randomUUID().toString(),
            displayValue = value,
            rawValue = rawValue,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _history.value.orEmpty().toMutableList()
        currentList.add(0, newItem)
        _history.value = currentList
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
