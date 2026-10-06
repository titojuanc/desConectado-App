package com.desconectado.app.domain.model

import java.time.Instant

/** Snapshot de un resultado de desafío que se muestra en el historial del perfil. */
data class DesafioHecho(
    val titulo: String,
    val puntos: Int,
    val fecha: Instant,
    val estado: ChallengeResult.Status = ChallengeResult.Status.COMPLETED,
)
