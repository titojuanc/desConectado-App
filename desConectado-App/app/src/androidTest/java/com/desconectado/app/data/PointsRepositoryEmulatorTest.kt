package com.desconectado.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desconectado.app.AppContainer
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.data.points.FirestorePointsRepository
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.errorOrNull
import com.desconectado.app.util.EmuladorFirebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Puntos y desafíos hechos contra el emulador de Firestore (FR-027 a FR-033). Los movimientos se
 * cargan con credenciales de administrador porque la app no puede escribirlos. Requiere
 * `-PuseEmulator=true`.
 */
@RunWith(AndroidJUnit4::class)
class PointsRepositoryEmulatorTest {

    private lateinit var container: AppContainer
    private lateinit var uid: String

    @Before
    fun preparar() = runBlocking {
        assumeTrue("Requiere -PuseEmulator=true", BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        container = app.container
        container.auth.signOut()
        EmuladorFirebase.limpiarAuth()
        val registro = container.authRepository.registrar("Ana Prueba", "ana@mail.com", "Secreto123")
        assertEquals(Resultado.Exito(Unit), registro)
        uid = container.auth.currentUser!!.uid
    }

    private fun movimiento(id: String, tipo: String, monto: Int, titulo: String, dia: Int) {
        EmuladorFirebase.cargarDocumento(
            "users/$uid/movements",
            id,
            mapOf(
                "type" to ("stringValue" to tipo),
                "amount" to ("integerValue" to monto.toString()),
                "challengeTitle" to ("stringValue" to titulo),
                "createdAt" to ("timestampValue" to "2026-09-%02dT10:00:00Z".format(dia)),
            ),
        )
    }

    @Test
    fun unaCuentaNuevaNoTieneDesafiosHechos_ListaVaciaConExito() = runBlocking {
        val resultado = container.pointsRepository.ultimosDesafiosHechos(uid)

        assertEquals(Resultado.Exito(emptyList<Any>()), resultado)
    }

    @Test
    fun devuelveLosCincoAcreditacionesMasRecientes_EnOrdenDescendenteYSinCanjes() = runBlocking {
        for (dia in 10..15) movimiento("m$dia", "credit", dia, "Desafío $dia", dia)
        movimiento("canje", "redeem", 5, "Canje", 28)

        val resultado = container.pointsRepository.ultimosDesafiosHechos(uid)

        assertTrue("Falló la lectura: $resultado", resultado is Resultado.Exito)
        val desafios = (resultado as Resultado.Exito).valor
        assertEquals(listOf("Desafío 15", "Desafío 14", "Desafío 13", "Desafío 12", "Desafío 11"), desafios.map { it.titulo })
        assertEquals(listOf(15, 14, 13, 12, 11), desafios.map { it.puntos })
    }

    @Test
    fun laCuentaRecienRegistradaTieneSaldo0EnElPerfilYEnFirestore() = runBlocking {
        val perfil = container.perfilRepository.perfil(uid)

        assertEquals(0, (perfil as Resultado.Exito).valor.puntos)
        val guardado = container.firestore.collection("users").document(uid).get(Source.SERVER).await()
        assertEquals(0L, guardado.getLong("pointsBalance"))
    }

    @Test
    fun unPerfilAnteriorAlAjusteSinPointsBalanceSeLeeComoCero() = runBlocking {
        // Como lo dejaba el APK de la entrega 1: el mismo perfil, sin el campo del saldo.
        EmuladorFirebase.borrarDocumento("users/$uid")
        EmuladorFirebase.cargarDocumento(
            "users",
            uid,
            mapOf(
                "username" to ("stringValue" to "Ana Prueba"),
                "email" to ("stringValue" to "ana@mail.com"),
                "createdAt" to ("timestampValue" to "2026-09-19T10:00:00Z"),
            ),
        )

        val perfil = container.perfilRepository.perfil(uid)

        assertEquals(0, (perfil as Resultado.Exito).valor.puntos)
    }

    @Test
    fun elSaldoDelPerfilCoincideConLaSumaDeSusAcreditaciones() = runBlocking {
        EmuladorFirebase.borrarDocumento("users/$uid")
        EmuladorFirebase.cargarDocumento(
            "users",
            uid,
            mapOf(
                "username" to ("stringValue" to "Ana Prueba"),
                "email" to ("stringValue" to "ana@mail.com"),
                "createdAt" to ("timestampValue" to "2026-09-19T10:00:00Z"),
                "pointsBalance" to ("integerValue" to "60"),
            ),
        )
        movimiento("a", "credit", 10, "Salir a caminar", 20)
        movimiento("b", "credit", 20, "Andar en bici", 21)
        movimiento("c", "credit", 30, "Salir a trotar", 22)

        val saldo = (container.perfilRepository.perfil(uid) as Resultado.Exito).valor.puntos
        val suma = (container.pointsRepository.ultimosDesafiosHechos(uid) as Resultado.Exito).valor.sumOf { it.puntos }

        assertEquals(suma, saldo)
    }

    @Test
    fun sinAlcanzarElServidor_devuelveSinConexionYNoUnaListaVacia() = runBlocking {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val firestoreSinRed = FirebaseFirestore.getInstance(EmuladorFirebase.appSinRed(contexto))

        val resultado = FirestorePointsRepository(firestoreSinRed).ultimosDesafiosHechos(uid)

        assertEquals(ErrorApp.SinConexion, resultado.errorOrNull())
    }
}
