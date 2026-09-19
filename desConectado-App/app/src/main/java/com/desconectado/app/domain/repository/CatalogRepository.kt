package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado

/** Catálogos de solo lectura provistos por la plataforma. Sin conexión devuelven `SinConexion`. */
interface CatalogRepository {
    /** Desafíos ordenados por `order` ascendente, leídos desde el servidor. */
    suspend fun desafios(): Resultado<List<Desafio>>

    /** Recompensas ordenadas por `order` ascendente, leídas desde el servidor. */
    suspend fun recompensas(): Resultado<List<Recompensa>>
}
