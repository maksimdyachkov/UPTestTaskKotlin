package com.example.uptesttaskkotlin.model.repository

import androidx.lifecycle.LiveData
import com.example.uptesttaskkotlin.model.BarcodeItem

/** Stores the history of scanned barcodes, newest first. */
interface BarcodeRepository {

    val history: LiveData<List<BarcodeItem>>

    fun addBarcode(value: String, rawValue: String?)

    fun clearHistory()
}
