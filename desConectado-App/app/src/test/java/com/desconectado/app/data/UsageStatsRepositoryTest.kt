package com.desconectado.app.data

import android.content.pm.ApplicationInfo
import com.desconectado.app.data.usage.AndroidUsageStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageStatsRepositoryTest {
    @Test fun listaObjetivoIncluyeLasAppsSolicitadasYExcluyeWhatsapp() {
        val paquetes = AndroidUsageStatsRepository.TARGET_PACKAGES
        assertEquals(
            setOf(
                "com.discord",
                "com.instagram.android",
                "com.instagram.barcelona",
                "com.twitter.android",
                "com.google.android.youtube",
                "com.snapchat.android",
                "com.netflix.mediaclient",
                "com.disney.disneyplus",
                "com.wbd.stream",
                "com.amazon.avod.thirdpartyclient",
                "com.cbs.app",
                "com.crunchyroll.crunchyroid",
                "tv.pluto.android",
                "com.pinterest",
                "com.zhiliaoapp.musically",
                "com.google.android.play.games",
                "com.facebook.katana",
            ),
            paquetes,
        )
        assertTrue("com.instagram.android" in paquetes)
        assertTrue("com.zhiliaoapp.musically" in paquetes)
        assertTrue("com.facebook.katana" in paquetes)
        assertTrue("com.twitter.android" in paquetes)
        assertTrue("com.snapchat.android" in paquetes)
        assertTrue("com.google.android.youtube" in paquetes)
        assertFalse("com.whatsapp" in paquetes)
    }

    @Test fun clasificaComoProhibidosTodosLosPaquetesDeJuegos() {
        assertTrue(
            AndroidUsageStatsRepository.isTargetPackage(
                "com.ejemplo.juego",
                ApplicationInfo.CATEGORY_GAME,
            ),
        )
        assertFalse(
            AndroidUsageStatsRepository.isTargetPackage(
                "com.whatsapp",
                ApplicationInfo.CATEGORY_UNDEFINED,
            ),
        )
    }
}
