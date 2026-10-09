package com.desconectado.app.domain.model

import java.time.Instant

enum class AchievementCriterion(val valorAlmacen: String) {
    COMPLETED_CHALLENGES("completed_challenges"),
    COMPLETED_SECONDS("completed_seconds"),
    SINGLE_CHALLENGE_SECONDS("single_challenge_seconds"),
    CATEGORY_COMPLETIONS("category_completions"),
    EXPLORED_CATEGORIES("explored_categories"),
    ;

    companion object {
        fun desdeAlmacen(valor: String?): AchievementCriterion? = entries.firstOrNull { it.valorAlmacen == valor }
    }
}

data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val criterion: AchievementCriterion,
    val threshold: Int,
    val order: Int,
    val category: CategoriaDesafio? = null,
    val active: Boolean = true,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(description.isNotBlank())
        require(threshold > 0)
        require(order > 0)
        require((criterion == AchievementCriterion.CATEGORY_COMPLETIONS) == (category != null))
    }
}

data class AchievementProgress(
    val definition: AchievementDefinition,
    val progress: Int,
) {
    val threshold: Int get() = definition.threshold
    val unlocked: Boolean get() = progress >= threshold
}

data class UserAchievement(
    val achievementId: String,
    val progress: Int,
    val threshold: Int,
    val unlockedAt: Instant? = null,
)