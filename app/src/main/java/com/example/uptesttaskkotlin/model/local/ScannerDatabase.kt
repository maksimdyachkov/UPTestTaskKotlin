package com.example.uptesttaskkotlin.model.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BarcodeEntity::class], version = 1)
abstract class ScannerDatabase : RoomDatabase() {

    abstract fun barcodeDao(): BarcodeDao

    companion object {
        const val NAME = "scanner.db"
    }
}
