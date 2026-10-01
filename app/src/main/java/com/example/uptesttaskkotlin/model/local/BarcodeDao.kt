package com.example.uptesttaskkotlin.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {

    /**
     * Last inserted first. Ordering by rowid instead of the timestamp keeps the order stable
     * when the device clock is changed.
     */
    @Query("SELECT * FROM barcodes ORDER BY rowid DESC")
    fun observeAll(): Flow<List<BarcodeEntity>>

    @Insert
    suspend fun insert(barcode: BarcodeEntity)

    @Query("DELETE FROM barcodes")
    suspend fun deleteAll()
}
