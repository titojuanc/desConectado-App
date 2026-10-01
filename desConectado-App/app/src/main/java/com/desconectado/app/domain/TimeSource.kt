package com.desconectado.app.domain

import java.time.Instant

interface TimeSource {
    fun now(): Instant
    fun elapsedSeconds(from: Instant, to: Instant = now()): Long =
        (to.epochSecond - from.epochSecond).coerceAtLeast(0)
}

object SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()
}
