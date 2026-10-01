package com.example.uptesttaskkotlin.model.repository

import com.example.uptesttaskkotlin.model.BarcodeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [BarcodeRepository] for unit tests. */
class FakeBarcodeRepository : BarcodeRepository {

    private val items = MutableStateFlow<List<BarcodeItem>>(emptyList())
    override val history: Flow<List<BarcodeItem>> = items

    override suspend fun addBarcode(value: String, rawValue: String?) {
        val item = BarcodeItem(
            id = (items.value.size + 1).toString(),
            displayValue = value,
            rawValue = rawValue,
            timestamp = 0L
        )
        items.value = listOf(item) + items.value
    }

    override suspend fun clearHistory() {
        items.value = emptyList()
    }
}
