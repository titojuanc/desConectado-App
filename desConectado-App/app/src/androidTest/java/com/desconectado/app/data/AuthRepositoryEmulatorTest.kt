package com.desconectado.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.AppContainer
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.util.EmuladorFirebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `AuthRepository` y `ProfileRepository` contra los emuladores de Firebase.
 * Requiere `-PuseEmulator=true` y los emuladores de Auth y Firestore activos.
 */
@RunWith(AndroidJUnit4::class)
class AuthRepositoryEmulatorTest {

    private lateinit var container: AppContainer

    @Before
    fun preparar() {
        assumeTrue("Requiere -PuseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        container = app.container
        container.auth.signOut()
        // Solo se limpian las cuentas: cada prueba crea uids nuevos, y los catálogos sembrados en
        // Firestore no deben borrarse (los usan otras pruebas).
        EmuladorFirebase.limpiarAuth()
    }

    private val auth get() = container.authRepository

    private suspend fun estadoActual(): EstadoSesion = withTimeout(TIEMPO_MAXIMO) {
        auth.authState.first { it !is EstadoSesion.Cargando }
    }

    @Test
    fun registrar_creaCuentaYPerfilYDejaLaSesionIniciada() = runBlocking {
        val resultado = auth.registrar("Ana Prueba", "Ana@Mail.com", "Secreto123")

        assertEquals(Resultado.Exito(Unit), resultado)
        val sesion = estadoActual()
        assertTrue("Se esperaba una sesión iniciada pero fue $sesion", sesion is EstadoSesion.ConSesion)
        val uid = (sesion as EstadoSesion.ConSesion).uid
        assertEquals(
            Resultado.Exito(Perfil(username = "Ana Prueba", email = "ana@mail.com")),
            container.perfilRepository.perfil(uid),
        )
    }

    @Test
    fun registrar_conUnCorreoRepetido_daCorreoEnUso() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")
        auth.cerrarSesion()

        val resultado = auth.registrar("Otra Ana", "ANA@mail.com", "OtraClave123")

        assertEquals(ErrorApp.CorreoEnUso, resultado.errorOrNull())
    }

    @Test
    fun ingresar_conContrasenaIncorrectaOCorreoInexistente_daCredencialesInvalidas() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")
        auth.cerrarSesion()

        val passwordIncorrecta = auth.ingresar("ana@mail.com", "Incorrecta1")
        val correoInexistente = auth.ingresar("nadie@mail.com", "Secreto123")

        assertEquals(ErrorApp.CredencialesInvalidas, passwordIncorrecta.errorOrNull())
        assertEquals(ErrorApp.CredencialesInvalidas, correoInexistente.errorOrNull())
    }

    @Test
    fun ingresar_ignoraMayusculasYEspaciosDelCorreo() = runBlocking {
        auth.registrar("Ana", "  Ana@Mail.COM ", "Secreto123")
        auth.cerrarSesion()

        assertEquals(Resultado.Exito(Unit), auth.ingresar("ana@mail.com", "Secreto123"))
        auth.cerrarSesion()
        assertEquals(Resultado.Exito(Unit), auth.ingresar("  ANA@mail.com  ", "Secreto123"))
    }

    @Test
    fun ingresar_creaElPerfilSiElRegistroAnteriorQuedoAMedias() = runBlocking {
        // Cuenta creada directamente en Auth, sin perfil (registro interrumpido).
        container.auth.createUserWithEmailAndPassword("ana@mail.com", "Secreto123").await()
        val uid = container.auth.currentUser!!.uid
        container.auth.signOut()

        assertEquals(Resultado.Exito(Unit), auth.ingresar("ana@mail.com", "Secreto123"))

        assertEquals(
            Resultado.Exito(Perfil(username = "ana", email = "ana@mail.com")),
            container.perfilRepository.perfil(uid),
        )
    }

    @Test
    fun cerrarSesion_dejaSinSesion() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")
        assertTrue(estadoActual() is EstadoSesion.ConSesion)

        auth.cerrarSesion()

        assertEquals(EstadoSesion.SinSesion, estadoActual())
    }

    @Test
    fun verificarCuenta_cierraLaSesionSiLaCuentaSeElimino() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")
        val token = container.auth.currentUser!!.getIdToken(false).await().token
        assertNotNull(token)

        EmuladorFirebase.eliminarCuenta(token!!)
        auth.verificarCuenta()

        assertNull(container.auth.currentUser)
        assertEquals(EstadoSesion.SinSesion, estadoActual())
    }

    @Test
    fun verificarCuenta_conLaCuentaVigente_noCierraLaSesion() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")

        auth.verificarCuenta()

        assertNotNull(container.auth.currentUser)
    }

    private companion object {
        const val TIEMPO_MAXIMO = 10_000L
    }
}
