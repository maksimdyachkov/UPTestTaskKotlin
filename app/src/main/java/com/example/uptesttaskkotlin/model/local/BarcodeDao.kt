package com.example.uptesttaskkotlin.model.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {

    /** Newest first; rowid breaks ties between scans saved within the same millisecond. */
    @Query("SELECT * FROM barcodes ORDER BY timestamp DESC, rowid DESC")
    fun observeAll(): Flow<List<BarcodeEntity>>

    @Insert
    suspend fun insert(barcode: BarcodeEntity)

    @Query("DELETE FROM barcodes")
    suspend fun deleteAll()
}
