package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Resultado

/**
 * Movimientos de puntos de la persona, en `users/{uid}/movements`. Solo lectura: no existe ninguna
 * operación que escriba puntos (FR-032); las reglas de seguridad tampoco lo permiten.
 */
interface PointsRepository {
    /**
     * Los últimos desafíos hechos (movimientos `credit`), del más reciente al más antiguo, hasta
     * `limite`. Lee del servidor: sin conexión devuelve `SinConexion`, nunca datos en caché ni una
     * lista vacía (FR-033). Una lista vacía con éxito significa "todavía no hay desafíos hechos".
     */
    suspend fun ultimosDesafiosHechos(uid: String, limite: Int = 5): Resultado<List<DesafioHecho>>
}
