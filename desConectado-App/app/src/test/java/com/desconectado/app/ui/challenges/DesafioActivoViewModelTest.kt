package com.desconectado.app.ui.challenges

import android.content.ContextWrapper
import androidx.lifecycle.viewModelScope
import com.desconectado.app.data.challenges.ActiveChallengeStore
import com.desconectado.app.data.notifications.DesconectadoNotifications
import com.desconectado.app.domain.model.ActiveChallenge
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ChallengeRepository
import com.desconectado.app.domain.repository.PointsRepository
import com.desconectado.app.fakes.FakeAchievementRepository
import com.desconectado.app.fakes.FakeChallengeRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.fakes.FakePointsRepository
import com.desconectado.app.fakes.FakeTimeSource
import com.desconectado.app.fakes.FakeUsageStatsRepository
import com.desconectado.app.fakes.FakeUserPreferencesRepository
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import kotlinx.coroutines.CancellationException
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DesafioActivoViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val inicio = Instant.parse("2026-10-14T10:00:00Z")
    private val activo = ActiveChallenge(
        challengeId = "d1",
        challengeTitle = "Caminar",
        durationMinutes = 1,
        points = 10,
        startedAt = inicio,
        offlineSeconds = 0,
        status = ActiveChallenge.Status.ACTIVE,
        updatedAt = inicio,
    )

    private fun crearViewModel(
        challenges: ChallengeRepository,
        points: PointsRepository = FakePointsRepository(),
        usage: FakeUsageStatsRepository = FakeUsageStatsRepository(),
    ): DesafioActivoViewModel {
        val context = ContextWrapper(null)
        return DesafioActivoViewModel(
            uid = "usuario",
            challenges = challenges,
            usage = usage,
            store = ActiveChallengeStore(context),
            points = points,
            achievements = FakeAchievementRepository(),
            userPreferences = FakeUserPreferencesRepository(),
            notifications = DesconectadoNotifications(context),
            connectivity = FakeConnectivityMonitor(),
            time = FakeTimeSource(inicio.plusSeconds(60)),
        )
    }

    @Test
    fun cancelacionFallidaTambienQuedaProtegidaYReintentable() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        var recibido: ChallengeResult? = null
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> {
                recibido = result
                throw IllegalStateException("Fallo al cancelar")
            }
        }
        val vm = crearViewModel(challenges)
        try {
            vm.cancelar()
            assertEquals(ChallengeResult.Status.CANCELLED, recibido?.status)
            assertEquals(0, recibido?.pointsAwarded)
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun invalidacionPorPermisoConservaMotivoAunqueFalleElGuardado() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        var recibido: ChallengeResult? = null
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> {
                recibido = result
                return Resultado.Fallo(ErrorApp.FirestoreNoAutorizado)
            }
        }
        val vm = crearViewModel(challenges, usage = FakeUsageStatsRepository(access = false))
        try {
            vm.actualizar()
            assertEquals(ChallengeResult.Status.INVALIDATED, recibido?.status)
            assertEquals("usage_access_revoked", recibido?.invalidationReason)
            assertEquals(DesafioActivoUiState.Error(ErrorApp.FirestoreNoAutorizado, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun cancelacionDeCorrutinaNoSeConvierteEnErrorDeNegocio() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> =
                throw CancellationException("Pantalla destruida")
        }
        val vm = crearViewModel(challenges)
        try {
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Completando, vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun excepcionAlGuardarMuestraErrorReintentableSinCerrarLaApp() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        var intentos = 0
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> {
                intentos++
                if (intentos == 1) throw IllegalStateException("Fallo simulado al guardar")
                return Resultado.Fallo(ErrorApp.FirestoreNoAutorizado)
            }
        }
        val vm = crearViewModel(challenges)
        try {
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
            vm.volverCatalogo()
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
            vm.finalizar()
            assertEquals(2, intentos)
            assertEquals(DesafioActivoUiState.Error(ErrorApp.FirestoreNoAutorizado, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun excepcionDeAcreditacionNoSePresentaComoExito() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult) = Resultado.Exito(result)
        }
        val points = object : PointsRepository by FakePointsRepository() {
            override suspend fun acreditarConRacha(
                uid: String, result: ChallengeResult, historial: List<ChallengeResult>,
            ): Resultado<ChallengeResult> = throw IllegalArgumentException("Datos de crédito inválidos")
        }
        val vm = crearViewModel(challenges, points)
        try {
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun errorDeAlmacenamientoNoCierraLaAppNiPierdeElReintento() = runTest {
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult): Resultado<ChallengeResult> =
                throw java.io.IOException("Almacenamiento no disponible")
        }
        val vm = crearViewModel(challenges)
        try {
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun iniciarMuestraProcesamientoYEvitaSolicitudesDuplicadas() = runTest {
        val inicioPendiente = CompletableDeferred<Resultado<ActiveChallenge>>()
        val base = FakeChallengeRepository().apply {
            activeResult = Resultado.Fallo(ErrorApp.Desconocido)
        }
        var inicios = 0
        val challenges = object : ChallengeRepository by base {
            override suspend fun start(uid: String, challengeId: String): Resultado<ActiveChallenge> {
                inicios++
                return inicioPendiente.await()
            }
        }
        val vm = crearViewModel(challenges)
        val desafio = Desafio("d1", "Caminar", "", 1, Dificultad.FACIL, 10, 1)
        try {
            vm.iniciar(desafio)
            assertEquals(DesafioActivoUiState.Iniciando, vm.estado.value)
            vm.iniciar(desafio)
            vm.volverCatalogo()
            assertEquals(1, inicios)
            assertEquals(DesafioActivoUiState.Iniciando, vm.estado.value)

            inicioPendiente.complete(Resultado.Fallo(ErrorApp.SinConexion))
            assertEquals(DesafioActivoUiState.SinConexion, vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun muestraCompletandoMientrasGuardaYNoPermiteOtroInicioNiCerrar() = runTest {
        val guardado = CompletableDeferred<Resultado<ChallengeResult>>()
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        var inicios = 0
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult) = guardado.await()
            override suspend fun start(uid: String, challengeId: String): Resultado<ActiveChallenge> {
                inicios++
                return Resultado.Exito(activo)
            }
        }
        val vm = crearViewModel(challenges)
        try {
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Completando, vm.estado.value)

            vm.volverCatalogo()
            vm.iniciar(Desafio("d2", "Leer", "", 1, Dificultad.FACIL, 10, 1))
            vm.actualizar()
            assertEquals(DesafioActivoUiState.Completando, vm.estado.value)
            assertEquals(0, inicios)

            guardado.complete(Resultado.Fallo(ErrorApp.SinConexion))
            assertEquals(DesafioActivoUiState.Error(ErrorApp.SinConexion, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun mantieneCompletandoHastaQueLaAcreditacionRespondeYMuestraSuError() = runTest {
        val acreditacion = CompletableDeferred<Resultado<Unit>>()
        val base = FakeChallengeRepository().apply { activeResult = Resultado.Exito(activo) }
        val challenges = object : ChallengeRepository by base {
            override suspend fun finish(uid: String, runId: String, result: ChallengeResult) = Resultado.Exito(result)
        }
        var creditos = 0
        val points = object : PointsRepository by FakePointsRepository() {
            override suspend fun acreditarConRacha(
                uid: String,
                result: ChallengeResult,
                historial: List<ChallengeResult>,
            ): Resultado<ChallengeResult> {
                creditos++
                return when (val credito = acreditacion.await()) {
                    is Resultado.Exito -> Resultado.Exito(result)
                    is Resultado.Fallo -> credito
                }
            }
        }
        val vm = crearViewModel(challenges, points)
        try {
            vm.actualizar()
            assertEquals(1, creditos)
            assertEquals(DesafioActivoUiState.Completando, vm.estado.value)

            acreditacion.complete(Resultado.Fallo(ErrorApp.Desconocido))
            assertEquals(DesafioActivoUiState.Error(ErrorApp.Desconocido, alFinalizar = true), vm.estado.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
