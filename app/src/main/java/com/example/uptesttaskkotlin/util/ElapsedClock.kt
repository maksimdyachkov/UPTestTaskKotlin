package com.example.uptesttaskkotlin.util

/** Source of monotonic time in milliseconds, unaffected by wall clock changes. */
fun interface ElapsedClock {
    fun now(): Long
}
