package com.desconectado.app.data

import com.desconectado.app.data.usage.AndroidUsageStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageStatsRepositoryTest {
    @Test fun listaObjetivoContieneLasSeisAppsYExcluyeWhatsapp() {
        val paquetes = AndroidUsageStatsRepository.TARGET_PACKAGES
        assertEquals(6, paquetes.size)
        assertTrue("com.instagram.android" in paquetes)
        assertTrue("com.zhiliaoapp.musically" in paquetes)
        assertTrue("com.facebook.katana" in paquetes)
        assertTrue("com.twitter.android" in paquetes)
        assertTrue("com.snapchat.android" in paquetes)
        assertTrue("com.google.android.youtube" in paquetes)
        assertFalse("com.whatsapp" in paquetes)
    }
}
