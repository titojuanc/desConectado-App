package com.desconectado.app.fakes

import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ChallengeRepository

class FakeChallengeRepository : ChallengeRepository {
    var historyResult: Resultado<List<DesafioHecho>> = Resultado.Exito(emptyList())
    val historyCalls = mutableListOf<String>()
    var activeResult: Resultado<ActiveChallenge?> = Resultado.Exito(null)
    var startResult: Resultado<ActiveChallenge> = Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.Desconocido)
    var finishResult: Resultado<ChallengeResult> = Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.Desconocido)
    override suspend fun history(uid: String): Resultado<List<DesafioHecho>> {
        historyCalls += uid
        return historyResult
    }
    override suspend fun active(uid: String) = activeResult
    override suspend fun start(uid: String, challengeId: String) = startResult
    override suspend fun updateOffline(uid: String, runId: String, seconds: Long) = Resultado.Exito(Unit)
    override suspend fun finish(uid: String, runId: String, result: ChallengeResult) = finishResult
    override suspend fun cancel(uid: String, runId: String) = finishResult
    override suspend fun invalidate(uid: String, runId: String, reason: String) = finishResult
}
