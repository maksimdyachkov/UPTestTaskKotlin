package com.example.uptesttaskkotlin.model.repository

import com.example.uptesttaskkotlin.model.BarcodeItem
import kotlinx.coroutines.flow.Flow

/** Stores the history of scanned barcodes, newest first. */
interface BarcodeRepository {

    val history: Flow<List<BarcodeItem>>

    suspend fun addBarcode(value: String, rawValue: String?)

    suspend fun clearHistory()
}
