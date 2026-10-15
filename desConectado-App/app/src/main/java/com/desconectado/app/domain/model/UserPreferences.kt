package com.desconectado.app.domain.model

import com.desconectado.app.domain.esCosmetico
import kotlin.math.roundToInt

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
        const val META_MAXIMA = 2520
        const val INCREMENTO_META = 30
        const val META_DIARIA_MINIMA = 30
        const val META_DIARIA_MAXIMA = 360

        fun metaSemanalDesdeDiaria(minutosDiarios: Int): Int {
            require(minutosDiarios in META_DIARIA_MINIMA..META_DIARIA_MAXIMA)
            require(minutosDiarios % INCREMENTO_META == 0)
            return minutosDiarios * 7
        }

        fun metaDiariaDesdeSemanal(minutosSemanales: Int): Int {
            require(minutosSemanales in META_MINIMA..META_MAXIMA)
            require(minutosSemanales % INCREMENTO_META == 0)
            return ((minutosSemanales / 7f / INCREMENTO_META).roundToInt() * INCREMENTO_META)
                .coerceIn(META_DIARIA_MINIMA, META_DIARIA_MAXIMA)
        }
    }
}