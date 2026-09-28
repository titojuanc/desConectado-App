package com.desconectado.app.fakes

import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.PointsRepository

/** Doble de `PointsRepository` con resultados programables y registro de llamadas (uid y límite). */
class FakePointsRepository : PointsRepository {

    var resultado: Resultado<List<DesafioHecho>> = Resultado.Exito(emptyList())

    val llamadas = mutableListOf<Pair<String, Int>>()

    override suspend fun ultimosDesafiosHechos(uid: String, limite: Int): Resultado<List<DesafioHecho>> {
        llamadas += uid to limite
        return resultado
    }
}
