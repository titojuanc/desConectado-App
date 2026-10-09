package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.CategoriaDesafio
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class ProgressMetrics(
    val desafiosCompletados: Int,
    val tiempoCompletadoSegundos: Long,
    val rachaDias: Int,
    val tiempoSemanalSegundos: Long,
    val minutosRestantesMetaSemanal: Int?,
    val desafiosPorCategoria: Map<CategoriaDesafio, Int>,
)

object ProgressCalculator {
    fun calcular(
        resultados: List<ChallengeResult>,
        ahora: Instant,
        zonaActual: ZoneId,
        metaSemanalMinutos: Int? = null,
    ): ProgressMetrics {
        if (metaSemanalMinutos != null) {
            require(metaSemanalMinutos in META_MINIMA_MINUTOS..META_MAXIMA_MINUTOS)
            require(metaSemanalMinutos % INCREMENTO_META_MINUTOS == 0)
        }

        val completados = resultados.filter {
            it.status == ChallengeResult.Status.COMPLETED && !it.finishedAt.isAfter(ahora)
        }
        val diasCompletados = completados.map { resultado ->
            resultado.finishedAt.atZone(ZoneId.of(resultado.timeZoneId)).toLocalDate()
        }.toSet()
        val hoy = ahora.atZone(zonaActual).toLocalDate()
        val diaInicialRacha = when {
            hoy in diasCompletados -> hoy
            hoy.minusDays(1) in diasCompletados -> hoy.minusDays(1)
            else -> null
        }
        val racha = diaInicialRacha?.let { contarDiasConsecutivos(diasCompletados, it) } ?: 0

        val inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val segundosSemanales = completados
            .filter { resultado ->
                val fechaLocal = resultado.finishedAt
                    .atZone(ZoneId.of(resultado.timeZoneId))
                    .toLocalDate()
                fechaLocal >= inicioSemana && fechaLocal <= hoy
            }
            .sumOf { it.durationSeconds.toLong() }
        val minutosRestantes = metaSemanalMinutos?.let { meta ->
            val segundosRestantes = (meta * SEGUNDOS_POR_MINUTO - segundosSemanales).coerceAtLeast(0L)
            ((segundosRestantes + SEGUNDOS_POR_MINUTO - 1) / SEGUNDOS_POR_MINUTO).toInt()
        }

        return ProgressMetrics(
            desafiosCompletados = completados.size,
            tiempoCompletadoSegundos = completados.sumOf { it.durationSeconds.toLong() },
            rachaDias = racha,
            tiempoSemanalSegundos = segundosSemanales,
            minutosRestantesMetaSemanal = minutosRestantes,
            desafiosPorCategoria = completados.mapNotNull { it.category }.groupingBy { it }.eachCount(),
        )
    }

    private fun contarDiasConsecutivos(dias: Set<LocalDate>, desde: LocalDate): Int {
        var dia = desde
        var total = 0
        while (dia in dias) {
            total++
            dia = dia.minusDays(1)
        }
        return total
    }

    private const val SEGUNDOS_POR_MINUTO = 60L
    private const val INCREMENTO_META_MINUTOS = 30
    const val META_MINIMA_MINUTOS = 30
    const val META_MAXIMA_MINUTOS = 840
}