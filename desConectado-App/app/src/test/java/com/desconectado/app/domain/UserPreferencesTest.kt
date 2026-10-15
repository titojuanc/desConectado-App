package com.desconectado.app.domain

import com.desconectado.app.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class UserPreferencesTest {
    @Test
    fun noDefineMetaPorDefectoYNotificacionesEmpiezanApagadas() {
        val preferences = UserPreferences()

        assertNull(preferences.weeklyGoalMinutes)
        assertEquals(false, preferences.notificationsEnabled)
    }

    @Test
    fun aceptaExtremosDelRangoEnIncrementosDeTreinta() {
        assertEquals(30, UserPreferences(weeklyGoalMinutes = 30).weeklyGoalMinutes)
        assertEquals(2520, UserPreferences(weeklyGoalMinutes = 2520).weeklyGoalMinutes)
    }

    @Test
    fun rechazaMetasFueraDeRangoONoMultiploDeTreinta() {
        for (minutes in listOf(0, 15, 45, 2550)) {
            assertThrows(IllegalArgumentException::class.java) {
                UserPreferences(weeklyGoalMinutes = minutes)
            }
        }
    }

    @Test
    fun convierteMetaDiariaEnSemanalYNormalizaMetasSemanalesLegacy() {
        assertEquals(210, UserPreferences.metaSemanalDesdeDiaria(30))
        assertEquals(2520, UserPreferences.metaSemanalDesdeDiaria(360))
        assertEquals(30, UserPreferences.metaDiariaDesdeSemanal(30))
        assertEquals(120, UserPreferences.metaDiariaDesdeSemanal(840))
        assertEquals(360, UserPreferences.metaDiariaDesdeSemanal(2520))
    }
}