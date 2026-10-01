package com.example.uptesttaskkotlin.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.uptesttaskkotlin.model.repository.FakeBarcodeRepository
import com.example.uptesttaskkotlin.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    // LiveData.setValue() asserts the main thread via Looper, which does not exist on the JVM.
    // The rule swaps the Architecture Components executor for a synchronous one.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var now = 0L
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        now = 0L
        viewModel = MainViewModel(FakeBarcodeRepository(), clock = { now })
        // The history LiveData collects from the repository only while it is observed.
        viewModel.scanHistory.observeForever { }
    }

    @Test
    fun `initial state is not scanning with empty history`() {
        assertFalse(viewModel.isScanning.value!!)
        assertEquals(NO_RESULT, viewModel.currentScanResult.value)
        assertTrue(viewModel.scanHistory.value!!.isEmpty())
    }

    @Test
    fun `toggleScanning switches scanning on and off`() {
        viewModel.toggleScanning()
        assertTrue(viewModel.isScanning.value!!)

        viewModel.toggleScanning()
        assertFalse(viewModel.isScanning.value!!)
    }

    @Test
    fun `stopScanning turns scanning off`() {
        viewModel.toggleScanning()

        viewModel.stopScanning()

        assertFalse(viewModel.isScanning.value!!)
    }

    @Test
    fun `stopScanning keeps scanning off when it is already off`() {
        viewModel.stopScanning()

        assertFalse(viewModel.isScanning.value!!)
    }

    @Test
    fun `scanned barcode becomes the current result and is added to history`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertEquals(CODE_A, viewModel.currentScanResult.value)
        val item = viewModel.scanHistory.value!!.single()
        assertEquals(CODE_A, item.displayValue)
        assertEquals(RAW_A, item.rawValue)
    }

    @Test
    fun `barcode that stays in view is recorded once`() {
        // One detection per frame for much longer than the duplicate window.
        repeat(100) {
            viewModel.onBarcodeScanned(CODE_A, RAW_A)
            now += FRAME_INTERVAL_MS
        }

        assertEquals(1, viewModel.scanHistory.value!!.size)
    }

    @Test
    fun `same barcode is recorded again after it was out of view long enough`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)
        now += MainViewModel.DUPLICATE_WINDOW_MS

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertEquals(2, viewModel.scanHistory.value!!.size)
    }

    @Test
    fun `same barcode is ignored just before the duplicate window ends`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)
        now += MainViewModel.DUPLICATE_WINDOW_MS - 1

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertEquals(1, viewModel.scanHistory.value!!.size)
    }

    @Test
    fun `different barcode is recorded immediately and placed first`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        viewModel.onBarcodeScanned(CODE_B, null)

        assertEquals(CODE_B, viewModel.currentScanResult.value)
        assertEquals(listOf(CODE_B, CODE_A), viewModel.scanHistory.value!!.map { it.displayValue })
    }

    @Test
    fun `detected barcode emits an in-view event that can be handled only once`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        val event = viewModel.barcodeInViewEvent.value!!
        assertNotNull(event.getContentIfNotHandled())
        assertNull(event.getContentIfNotHandled())
    }

    @Test
    fun `repeated detection of the same barcode emits a new in-view event without a new history entry`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)
        viewModel.barcodeInViewEvent.value!!.getContentIfNotHandled()

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertNotNull(viewModel.barcodeInViewEvent.value!!.getContentIfNotHandled())
        assertEquals(1, viewModel.scanHistory.value!!.size)
    }

    @Test
    fun `clearHistory empties history and resets the current result`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        viewModel.clearHistory()

        assertTrue(viewModel.scanHistory.value!!.isEmpty())
        assertEquals(NO_RESULT, viewModel.currentScanResult.value)
    }

    @Test
    fun `barcode can be scanned again right after history is cleared`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)
        viewModel.clearHistory()

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertEquals(1, viewModel.scanHistory.value!!.size)
        assertEquals(CODE_A, viewModel.currentScanResult.value)
    }

    private companion object {
        const val NO_RESULT = "No barcode scanned yet"
        const val CODE_A = "https://ukrposhta.ua/track/A"
        const val RAW_A = "raw-A"
        const val CODE_B = "4820000000017"
        const val FRAME_INTERVAL_MS = 33L
    }
}
