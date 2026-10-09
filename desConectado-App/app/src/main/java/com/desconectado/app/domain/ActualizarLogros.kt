package com.desconectado.app.domain

import com.desconectado.app.domain.model.AchievementProgress
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AchievementRepository
import com.desconectado.app.domain.repository.ChallengeRepository

suspend fun actualizarLogrosTrasResultado(
    uid: String,
    resultado: ChallengeResult,
    desafios: ChallengeRepository,
    logros: AchievementRepository,
): Resultado<List<AchievementProgress>> {
    if (resultado.status != ChallengeResult.Status.COMPLETED) return Resultado.Exito(emptyList())
    return when (val historial = desafios.results(uid)) {
        is Resultado.Exito -> logros.actualizar(uid, historial.valor)
        is Resultado.Fallo -> historial
    }
}