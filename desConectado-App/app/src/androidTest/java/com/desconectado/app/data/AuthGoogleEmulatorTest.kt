package com.desconectado.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.AppContainer
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.util.EmuladorFirebase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Ingreso con Google contra el emulador de Auth, con tokens de Google falsos (el emulador acepta un
 * JSON como token). Es una aproximación automatizada de T-VERIF-1: el comportamiento real de
 * Firebase con Google se confirma a mano (T082).
 * Requiere `-PuseEmulator=true` y los emuladores de Auth y Firestore activos.
 */
@RunWith(AndroidJUnit4::class)
class AuthGoogleEmulatorTest {

    private lateinit var container: AppContainer

    @Before
    fun preparar() {
        assumeTrue("Requiere -PuseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        container = app.container
        container.auth.signOut()
        EmuladorFirebase.limpiarAuth()
    }

    private val auth get() = container.authRepository

    private fun tokenGoogle(sub: String, correo: String, nombre: String?): String =
        JSONObject()
            .put("sub", sub)
            .put("email", correo)
            .put("email_verified", true)
            .apply { if (nombre != null) put("name", nombre) }
            .toString()

    private fun uidActual(): String {
        val usuario = container.auth.currentUser
        assertNotNull("Se esperaba una sesión iniciada", usuario)
        return usuario!!.uid
    }

    @Test
    fun unTokenNuevo_creaCuentaYPerfilConElNombreDelToken() = runBlocking {
        val resultado = auth.ingresarConGoogle(tokenGoogle("g-1", "bea@mail.com", "Bea Google"))

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(
            Resultado.Exito(Perfil(username = "Bea Google", email = "bea@mail.com")),
            container.perfilRepository.perfil(uidActual()),
        )
    }

    @Test
    fun repetirElMismoToken_devuelveElMismoUid() = runBlocking {
        val token = tokenGoogle("g-1", "bea@mail.com", "Bea Google")
        auth.ingresarConGoogle(token)
        val primero = uidActual()
        auth.cerrarSesion()

        val resultado = auth.ingresarConGoogle(token)

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(primero, uidActual())
    }

    @Test
    fun unTokenSinNombre_creaElPerfilConLaParteLocalDelCorreo() = runBlocking {
        val resultado = auth.ingresarConGoogle(tokenGoogle("g-2", "carlos.perez@mail.com", null))

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(
            Resultado.Exito(Perfil(username = "carlos.perez", email = "carlos.perez@mail.com")),
            container.perfilRepository.perfil(uidActual()),
        )
    }

    @Test
    fun conElCorreoDeUnaCuentaDeContrasena_noCreaUnaCuentaDuplicada() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Secreto123")
        val uidContrasena = uidActual()
        auth.cerrarSesion()

        val resultado = auth.ingresarConGoogle(tokenGoogle("g-3", "ana@mail.com", "Ana Google"))

        // O Firebase unifica las cuentas (mismo uid) o se pide vincular; nunca hay dos cuentas.
        if (resultado is Resultado.Exito) {
            assertEquals(uidContrasena, uidActual())
        } else {
            assertEquals(ErrorApp.CuentaExistenteConOtroProveedor, resultado.errorOrNull())
        }
    }
}
