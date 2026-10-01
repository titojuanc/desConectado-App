package com.desconectado.app.fakes

import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.PointsRepository

/** Doble de `PointsRepository` con resultados programables y registro de llamadas (uid y límite). */
class FakePointsRepository : PointsRepository {

    var resultado: Resultado<List<DesafioHecho>> = Resultado.Exito(emptyList())
    var saldoResultado: Resultado<Int> = Resultado.Exito(0)
    var canjeadasResultado: Resultado<List<RedeemedReward>> = Resultado.Exito(emptyList())
    var canjeResultado: Resultado<RedeemedReward> = Resultado.Fallo(com.desconectado.app.domain.model.ErrorApp.Desconocido)
    var ultimoCanje: Triple<String, Recompensa, String>? = null

    val llamadas = mutableListOf<Pair<String, Int>>()

    override suspend fun ultimosDesafiosHechos(uid: String, limite: Int): Resultado<List<DesafioHecho>> {
        llamadas += uid to limite
        return resultado
    }

    override suspend fun saldo(uid: String): Resultado<Int> = saldoResultado
    override suspend fun recompensasCanjeadas(uid: String): Resultado<List<RedeemedReward>> = canjeadasResultado
    override suspend fun redeem(uid: String, reward: Recompensa, redemptionId: String): Resultado<RedeemedReward> {
        ultimoCanje = Triple(uid, reward, redemptionId)
        return canjeResultado
    }
}
