package com.desconectado.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Funciones puras de puntos (FR-028, FR-029, FR-031). Viven en `domain` para probarlas en la JVM.

/** Número completo con separador de miles: 0, 999, "1.250", "1.234.567". Nunca muestra negativos. */
fun formatearPuntos(puntos: Int): String =
    maxOf(puntos, 0).toString().reversed().chunked(3).joinToString(".").reversed()

/**
 * Saldo listo para mostrar a partir del valor guardado: ausente vale 0 (cuentas anteriores al ajuste
 * 2026-09-23) y nunca es negativo ni desborda el entero (FR-030, FR-031).
 */
fun normalizarSaldo(guardado: Long?): Int = (guardado ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Fecha de un desafío hecho, por ejemplo "22/09/2026", en la zona horaria indicada. */
fun formatearFechaDesafio(fecha: Instant, zona: ZoneId = ZoneId.systemDefault()): String =
    FORMATO_FECHA.format(fecha.atZone(zona))
