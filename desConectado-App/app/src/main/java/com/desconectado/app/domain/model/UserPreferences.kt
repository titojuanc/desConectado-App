package com.desconectado.app.domain.model

import com.desconectado.app.domain.esCosmetico

data class UserPreferences(
    val weeklyGoalMinutes: Int? = null,
    val notificationsEnabled: Boolean = false,
    val activeCosmetics: Map<TipoRecompensa, String> = emptyMap(),
) {
    init {
        require(weeklyGoalMinutes == null || weeklyGoalMinutes in META_MINIMA..META_MAXIMA)
        require(weeklyGoalMinutes == null || weeklyGoalMinutes % INCREMENTO_META == 0)
        require(activeCosmetics.all { (kind, rewardId) -> kind.esCosmetico() && rewardId.isNotBlank() })
    }

    companion object {
        const val META_MINIMA = 30
        const val META_MAXIMA = 840
        const val INCREMENTO_META = 30
    }
}