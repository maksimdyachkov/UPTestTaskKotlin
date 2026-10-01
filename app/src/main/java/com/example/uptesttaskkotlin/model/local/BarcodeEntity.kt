package com.example.uptesttaskkotlin.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.uptesttaskkotlin.model.BarcodeItem

@Entity(tableName = "barcodes")
data class BarcodeEntity(
    @PrimaryKey val id: String,
    val displayValue: String,
    val rawValue: String?,
    val timestamp: Long
)

fun BarcodeEntity.toModel() = BarcodeItem(id, displayValue, rawValue, timestamp)

fun BarcodeItem.toEntity() = BarcodeEntity(id, displayValue, rawValue, timestamp)
