package com.desconectado.app.domain

import com.desconectado.app.domain.model.ChallengeRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChallengeRatingTest {
    @Test
    fun aceptaLosLimitesDeUnaACincoEstrellas() {
        assertEquals(1, ChallengeRating("run-1", 1).stars)
        assertEquals(5, ChallengeRating("run-1", 5).stars)
    }

    @Test
    fun rechazaPuntuacionesFueraDelRango() {
        assertThrows(IllegalArgumentException::class.java) { ChallengeRating("run-1", 0) }
        assertThrows(IllegalArgumentException::class.java) { ChallengeRating("run-1", 6) }
    }
}