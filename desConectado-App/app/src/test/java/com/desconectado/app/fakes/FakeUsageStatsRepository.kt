package com.desconectado.app.fakes

import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.UsageStatsRepository
import java.time.Instant

class FakeUsageStatsRepository(
    var access: Boolean = true,
    var result: Resultado<Long> = Resultado.Exito(0L),
) : UsageStatsRepository {
    var lastStart: Instant? = null
    var lastEnd: Instant? = null
    override fun hasUsageAccess(): Boolean = access
    override fun openUsageAccessSettings() = Unit
    override suspend fun socialUsageSeconds(start: Instant, end: Instant): Resultado<Long> {
        lastStart = start
        lastEnd = end
        return result
    }
}
