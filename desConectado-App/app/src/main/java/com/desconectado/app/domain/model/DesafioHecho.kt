package com.desconectado.app.domain.model

import java.time.Instant

/**
 * Un desafío hecho, tal como se muestra en el perfil (FR-029): viene de un movimiento de puntos de
 * tipo `credit`. `titulo` es el del desafío al momento de acreditar; `puntos` es mayor que 0.
 */
data class DesafioHecho(
    val titulo: String,
    val puntos: Int,
    val fecha: Instant,
)
