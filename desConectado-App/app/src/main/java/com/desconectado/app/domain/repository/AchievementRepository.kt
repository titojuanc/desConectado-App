package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado

interface AchievementRepository {
    suspend fun actualizar(uid: String, resultados: List<ChallengeResult>): Resultado<List<AchievementProgress>>
}