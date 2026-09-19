package com.desconectado.app.fakes

import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.CatalogRepository
import kotlinx.coroutines.CompletableDeferred

/** Doble de `CatalogRepository`: resultados programables, conteo de llamadas y compuerta para simular lentitud. */
class FakeCatalogRepository : CatalogRepository {

    var resultadoDesafios: Resultado<List<Desafio>> = Resultado.Exito(emptyList())
    var resultadoRecompensas: Resultado<List<Recompensa>> = Resultado.Exito(emptyList())

    /** Si no es nulo, cada consulta espera a que se complete antes de responder. */
    var compuerta: CompletableDeferred<Unit>? = null

    var llamadasDesafios = 0
        private set
    var llamadasRecompensas = 0
        private set

    override suspend fun desafios(): Resultado<List<Desafio>> {
        llamadasDesafios++
        compuerta?.await()
        return resultadoDesafios
    }

    override suspend fun recompensas(): Resultado<List<Recompensa>> {
        llamadasRecompensas++
        compuerta?.await()
        return resultadoRecompensas
    }
}
