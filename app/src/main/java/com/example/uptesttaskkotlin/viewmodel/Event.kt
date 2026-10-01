package com.example.uptesttaskkotlin.viewmodel

/**
 * A LiveData value that must be handled only once.
 *
 * LiveData re-delivers its last value to every new observer (e.g. after rotation),
 * which is wrong for one-off signals such as "a barcode was just scanned".
 */
class Event<out T>(private val content: T) {

    private var isHandled = false

    fun getContentIfNotHandled(): T? {
        if (isHandled) return null
        isHandled = true
        return content
    }
}
