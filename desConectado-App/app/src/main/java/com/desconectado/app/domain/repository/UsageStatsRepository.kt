package com.desconectado.app.domain.repository

import java.time.Instant
import com.desconectado.app.domain.model.Resultado

interface UsageStatsRepository {
    fun hasUsageAccess(): Boolean
    fun openUsageAccessSettings()
    suspend fun socialUsageSeconds(start: Instant, end: Instant): Resultado<Long>
    suspend fun socialUsageByPackageSeconds(start: Instant, end: Instant): Resultado<Map<String, Long>> =
        when (val total = socialUsageSeconds(start, end)) {
            is Resultado.Exito -> Resultado.Exito(mapOf("redes sociales" to total.valor))
            is Resultado.Fallo -> total
        }
}
