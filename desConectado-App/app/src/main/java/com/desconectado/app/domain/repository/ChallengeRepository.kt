package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado

interface ChallengeRepository {
    suspend fun active(uid: String): Resultado<ActiveChallenge?>
    suspend fun start(uid: String, challengeId: String): Resultado<ActiveChallenge>
    suspend fun updateOffline(uid: String, runId: String, seconds: Long): Resultado<Unit>
    suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult>
    suspend fun cancel(uid: String, runId: String): Resultado<ChallengeResult>
    suspend fun invalidate(uid: String, runId: String, reason: String): Resultado<ChallengeResult>
}
