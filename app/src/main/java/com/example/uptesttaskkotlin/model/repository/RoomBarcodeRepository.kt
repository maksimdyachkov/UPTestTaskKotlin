package com.example.uptesttaskkotlin.model.repository

import com.example.uptesttaskkotlin.model.BarcodeItem
import com.example.uptesttaskkotlin.model.local.BarcodeDao
import com.example.uptesttaskkotlin.model.local.BarcodeEntity
import com.example.uptesttaskkotlin.model.local.toEntity
import com.example.uptesttaskkotlin.model.local.toModel
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the history in a Room database, so it survives app restarts. */
@Singleton
class RoomBarcodeRepository @Inject constructor(
    private val barcodeDao: BarcodeDao
) : BarcodeRepository {

    override val history: Flow<List<BarcodeItem>> =
        barcodeDao.observeAll().map { entities -> entities.map(BarcodeEntity::toModel) }

    override suspend fun addBarcode(value: String, rawValue: String?) {
        val item = BarcodeItem(
            id = UUID.randomUUID().toString(),
            displayValue = value,
            rawValue = rawValue,
            timestamp = System.currentTimeMillis()
        )
        barcodeDao.insert(item.toEntity())
    }

    override suspend fun clearHistory() {
        barcodeDao.deleteAll()
    }
}
