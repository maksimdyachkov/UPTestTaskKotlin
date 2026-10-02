package com.example.uptesttaskkotlin.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.uptesttaskkotlin.model.repository.FakeBarcodeRepository
import com.example.uptesttaskkotlin.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        viewModel = newViewModel().also { it.toggleScanning() }
    }

    private fun newViewModel() = MainViewModel(FakeBarcodeRepository(), clock = { now }).also {
        // The history LiveData collects from the repository only while it is observed.
        it.scanHistory.observeForever { }
    }

    @Test
    fun `initial state is not scanning with empty history`() {
        val freshViewModel = newViewModel()

        assertFalse(freshViewModel.isScanning.value!!)
        assertNull(freshViewModel.currentScanResult.value)
        assertTrue(freshViewModel.scanHistory.value!!.isEmpty())
    }

    @Test
    fun `toggleScanning switches scanning on and off`() {
        val freshViewModel = newViewModel()

        freshViewModel.toggleScanning()
        assertTrue(freshViewModel.isScanning.value!!)

        freshViewModel.toggleScanning()
        assertFalse(freshViewModel.isScanning.value!!)
    }

    @Test
    fun `barcode reported while scanning is off is ignored`() {
        viewModel.toggleScanning()

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertNull(viewModel.currentScanResult.value)
        assertTrue(viewModel.scanHistory.value!!.isEmpty())
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
    fun `two barcodes that stay in view together are recorded once each`() {
        // Both codes are reported for every frame, in no particular order.
        repeat(100) { frame ->
            val codes = if (frame % 2 == 0) listOf(CODE_A, CODE_B) else listOf(CODE_B, CODE_A)
            codes.forEach { viewModel.onBarcodeScanned(it, null) }
            now += FRAME_INTERVAL_MS
        }

        assertEquals(2, viewModel.scanHistory.value!!.size)
    }

    @Test
    fun `barcode is not recorded again when another one is scanned in between`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)
        viewModel.onBarcodeScanned(CODE_B, null)

        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        assertEquals(listOf(CODE_B, CODE_A), viewModel.scanHistory.value!!.map { it.displayValue })
    }

    @Test
    fun `different barcode is recorded immediately and placed first`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        viewModel.onBarcodeScanned(CODE_B, null)

        assertEquals(CODE_B, viewModel.currentScanResult.value)
        assertEquals(listOf(CODE_B, CODE_A), viewModel.scanHistory.value!!.map { it.displayValue })
    }

    @Test
    fun `clearHistory empties history and resets the current result`() {
        viewModel.onBarcodeScanned(CODE_A, RAW_A)

        viewModel.clearHistory()

        assertTrue(viewModel.scanHistory.value!!.isEmpty())
        assertNull(viewModel.currentScanResult.value)
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
        const val CODE_A = "https://ukrposhta.ua/track/A"
        const val RAW_A = "raw-A"
        const val CODE_B = "4820000000017"
        const val FRAME_INTERVAL_MS = 33L
    }
}
