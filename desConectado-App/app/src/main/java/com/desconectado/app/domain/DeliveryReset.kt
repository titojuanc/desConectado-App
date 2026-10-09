package com.desconectado.app.domain

import java.time.Instant

fun desafioAnteriorAlReset(startedAt: Instant, resetAt: Instant): Boolean = !startedAt.isAfter(resetAt)