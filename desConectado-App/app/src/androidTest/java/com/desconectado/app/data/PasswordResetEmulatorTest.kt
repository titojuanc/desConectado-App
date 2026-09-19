package com.desconectado.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.AppContainer
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.util.EmuladorFirebase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Restablecimiento de contraseña contra el emulador de Auth.
 * Requiere `-PuseEmulator=true` y el emulador de Auth activo.
 */
@RunWith(AndroidJUnit4::class)
class PasswordResetEmulatorTest {

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

    @Test
    fun restablecerYIngresarConLaContrasenaNueva() = runBlocking {
        auth.registrar("Ana", "ana@mail.com", "Original123")
        auth.cerrarSesion()

        val resultado = auth.restablecerPassword("  Ana@Mail.COM ")
        assertEquals(Resultado.Exito(Unit), resultado)

        val codigo = EmuladorFirebase.codigosRestablecimiento()
            .lastOrNull { (correo, _) -> correo == "ana@mail.com" }
            ?.second
        assertTrue("El emulador no generó ningún código de restablecimiento", codigo != null)
        EmuladorFirebase.restablecerPassword(codigo!!, "Nueva12345")

        assertEquals(Resultado.Exito(Unit), auth.ingresar("ana@mail.com", "Nueva12345"))
        auth.cerrarSesion()
        assertEquals(ErrorApp.CredencialesInvalidas, auth.ingresar("ana@mail.com", "Original123").errorOrNull())
    }

    @Test
    fun unCorreoSinCuenta_devuelveExitoYNoGeneraNingunCodigo() = runBlocking {
        val antes = EmuladorFirebase.codigosRestablecimiento().size

        val resultado = auth.restablecerPassword("nadie@mail.com")

        assertEquals(Resultado.Exito(Unit), resultado)
        assertEquals(antes, EmuladorFirebase.codigosRestablecimiento().size)
    }

    // "Sin conexión" no se prueba aquí: no se puede apuntar una segunda instancia de Auth a un puerto
    // muerto porque `useEmulator` es global al proceso. La clasificación de los errores de red de
    // Auth (incluido el "internal error" de reCAPTCHA) se cubre en JVM: ErroresRedTest.
}
