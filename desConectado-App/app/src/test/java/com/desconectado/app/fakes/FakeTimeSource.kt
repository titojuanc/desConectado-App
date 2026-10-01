package com.desconectado.app.fakes

import com.desconectado.app.domain.TimeSource
import java.time.Instant

class FakeTimeSource(initial: Instant) : TimeSource {
    var current: Instant = initial
    override fun now(): Instant = current
}
