package com.desconectado.app.fakes

import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AchievementRepository

class FakeAchievementRepository : AchievementRepository {
    var resultado: Resultado<List<AchievementProgress>> = Resultado.Exito(emptyList())
    val llamadas = mutableListOf<Pair<String, List<ChallengeResult>>>()

    override suspend fun actualizar(
        uid: String,
        resultados: List<ChallengeResult>,
    ): Resultado<List<AchievementProgress>> {
        llamadas += uid to resultados
        return resultado
    }
}