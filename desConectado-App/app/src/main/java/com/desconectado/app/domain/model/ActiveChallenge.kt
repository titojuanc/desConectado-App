package com.desconectado.app.domain.model

import java.time.Instant

data class ActiveChallenge(
    val challengeId: String,
    val challengeTitle: String,
    val durationMinutes: Int,
    val points: Int,
    val startedAt: Instant,
    val offlineSeconds: Long,
    val status: Status,
    val updatedAt: Instant,
    val durationSeconds: Int = durationMinutes * 60,
    val category: CategoriaDesafio? = null,
) {
    enum class Status { ACTIVE, CANCELLED, FAILED, COMPLETED, INVALIDATED }
}
