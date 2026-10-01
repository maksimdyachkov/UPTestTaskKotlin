package com.example.uptesttaskkotlin.model.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BarcodeDaoTest {

    private lateinit var database: ScannerDatabase
    private lateinit var dao: BarcodeDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ScannerDatabase::class.java
        ).build()
        dao = database.barcodeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedBarcodeIsReturnedWithAllFields() = runTest {
        val barcode = BarcodeEntity("1", "https://ukrposhta.ua", null, 100L)

        dao.insert(barcode)

        assertEquals(listOf(barcode), dao.observeAll().first())
    }

    @Test
    fun barcodesAreReturnedLastInsertedFirst() = runTest {
        dao.insert(BarcodeEntity("first", "A", "raw-A", 100L))
        dao.insert(BarcodeEntity("second", "B", "raw-B", 200L))

        assertEquals(listOf("second", "first"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun orderDoesNotDependOnTimestamps() = runTest {
        // The device clock was set back between the two scans.
        dao.insert(BarcodeEntity("first", "A", null, 200L))
        dao.insert(BarcodeEntity("second", "B", null, 100L))

        assertEquals(listOf("second", "first"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun deleteAllRemovesEveryBarcode() = runTest {
        dao.insert(BarcodeEntity("1", "A", null, 100L))
        dao.insert(BarcodeEntity("2", "B", null, 200L))

        dao.deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }
}
