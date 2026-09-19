package com.desconectado.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.AppContainer
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.data.catalog.FirestoreCatalogRepository
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.util.EmuladorFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Catálogos contra el emulador de Firestore ya sembrado (`node firebase/seed/seed.mjs --emulator`).
 * Requiere `-PuseEmulator=true`.
 */
@RunWith(AndroidJUnit4::class)
class CatalogDesafiosEmulatorTest {

    private lateinit var container: AppContainer

    @Before
    fun preparar() = runBlocking {
        assumeTrue("Requiere -PuseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        container = app.container
        container.auth.signOut()
        EmuladorFirebase.limpiarAuth()
        // Los catálogos solo los lee una persona autenticada.
        val registro = container.authRepository.registrar("Ana Prueba", "ana@mail.com", "Secreto123")
        assertEquals(Resultado.Exito(Unit), registro)
    }

    @Test
    fun desafios_devuelveLosSeisDesafiosOrdenadosPorOrder() = runBlocking {
        val resultado = container.catalogRepository.desafios()

        assertTrue("Falló la lectura: $resultado", resultado is Resultado.Exito)
        val desafios = (resultado as Resultado.Exito).valor
        assertEquals("¿Está sembrado el emulador? (node firebase/seed/seed.mjs --emulator)", 6, desafios.size)
        assertEquals(desafios.sortedBy { it.order }, desafios)
        assertEquals(setOf(Dificultad.FACIL, Dificultad.NORMAL, Dificultad.DIFICIL), desafios.map { it.difficulty }.toSet())
        assertTrue(desafios.all { it.title.startsWith("No uses redes sociales por") })
    }

    @Test
    fun recompensas_devuelveLasCincoRecompensasOrdenadasPorOrder() = runBlocking {
        val resultado = container.catalogRepository.recompensas()

        assertTrue("Falló la lectura: $resultado", resultado is Resultado.Exito)
        val recompensas = (resultado as Resultado.Exito).valor
        assertEquals("¿Está sembrado el emulador? (node firebase/seed/seed.mjs --emulator)", 5, recompensas.size)
        assertEquals(recompensas.sortedBy { it.order }, recompensas)
        assertTrue(recompensas.all { it.costPoints > 0 && it.name.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun desafios_sinAlcanzarElServidor_devuelveSinConexionYNoDatosDeCache() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val firestoreSinRed = FirebaseFirestore.getInstance(EmuladorFirebase.appSinRed(contexto))

        val resultado = FirestoreCatalogRepository(firestoreSinRed).desafios()

        assertEquals(ErrorApp.SinConexion, resultado.errorOrNull())
    }
}
