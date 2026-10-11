package com.coffeedial.domain

import kotlin.time.TimeSource

private val timerOrigin = TimeSource.Monotonic.markNow()

/** Elapsed time comes from a monotonic clock, not the number of UI refreshes. */
class ShotTimer(
    initialMillis: Long = 0,
    initialFirstDrop: Long? = null,
    private val now: () -> Long = { timerOrigin.elapsedNow().inWholeMilliseconds }
) {
    private var accumulated = initialMillis.coerceIn(0, MAX_MILLIS)
    private var started: Long? = null
    var firstDropMillis: Long? = initialFirstDrop?.takeIf { it in 0..accumulated }
        private set
    val running: Boolean get() = started != null
    val elapsedMillis: Long
        get() = (accumulated + (started?.let { (now() - it).coerceAtLeast(0) } ?: 0))
            .coerceAtMost(MAX_MILLIS)
    fun start() {
        if (!running && elapsedMillis < MAX_MILLIS) started = now()
    }
    fun pause() {
        accumulated = elapsedMillis
        started = null
    }
    fun reset() {
        accumulated = 0
        started = null
        firstDropMillis = null
    }
    fun markFirstDrop() {
        if (running && firstDropMillis == null) firstDropMillis = elapsedMillis
    }
    fun clearFirstDrop() {
        firstDropMillis = null
    }
    companion object {
        const val MAX_MILLIS = 600_000L
    }
}
