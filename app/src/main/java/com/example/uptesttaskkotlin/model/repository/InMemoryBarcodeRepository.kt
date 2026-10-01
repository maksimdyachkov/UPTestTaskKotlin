package com.example.uptesttaskkotlin.model.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.uptesttaskkotlin.model.BarcodeItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the history only while the app process is alive. */
@Singleton
class InMemoryBarcodeRepository @Inject constructor() : BarcodeRepository {

    private val _history = MutableLiveData<List<BarcodeItem>>(emptyList())
    override val history: LiveData<List<BarcodeItem>> = _history

    override fun addBarcode(value: String, rawValue: String?) {
        val newItem = BarcodeItem(
            id = UUID.randomUUID().toString(),
            displayValue = value,
            rawValue = rawValue,
            timestamp = System.currentTimeMillis()
        )
        _history.value = listOf(newItem) + _history.value.orEmpty()
    }

    override fun clearHistory() {
        _history.value = emptyList()
    }
}
