package com.desconectado.app.domain.model

import java.time.Instant

data class ChallengeRating(
    val challengeRunId: String,
    val stars: Int,
    val createdAt: Instant? = null,
) {
    init {
        require(challengeRunId.isNotBlank())
        require(stars in MIN_STARS..MAX_STARS)
    }

    companion object {
        const val MIN_STARS = 1
        const val MAX_STARS = 5
    }
}