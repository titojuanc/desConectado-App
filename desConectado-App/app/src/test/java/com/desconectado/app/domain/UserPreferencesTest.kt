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
        assertEquals(840, UserPreferences(weeklyGoalMinutes = 840).weeklyGoalMinutes)
    }

    @Test
    fun rechazaMetasFueraDeRangoONoMultiploDeTreinta() {
        for (minutes in listOf(0, 15, 45, 870)) {
            assertThrows(IllegalArgumentException::class.java) {
                UserPreferences(weeklyGoalMinutes = minutes)
            }
        }
    }
}