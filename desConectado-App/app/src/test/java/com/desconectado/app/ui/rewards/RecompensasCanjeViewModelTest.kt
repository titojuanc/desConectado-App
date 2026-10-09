package com.desconectado.app.ui.rewards

import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.PendingRedemption
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.domain.model.CosmeticOwnership
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.fakes.FakeCatalogRepository
import com.desconectado.app.fakes.FakeCosmeticPreferencesRepository
import com.desconectado.app.fakes.FakeConnectivityMonitor
import com.desconectado.app.fakes.FakePointsRepository
import com.desconectado.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecompensasCanjeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val catalog = FakeCatalogRepository()
    private val points = FakePointsRepository()
    private val preferences = FakeCosmeticPreferencesRepository()
    private val connectivity = FakeConnectivityMonitor()
    private val reward = Recompensa("coupon", "Cupón", "Digital", 50, TipoRecompensa.CUPON, 1)

    @Test fun cargaCatalogoYCanjeadas() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.canjeadasResultado = Resultado.Exito(emptyList())
        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()
        assertTrue(vm.estado.value is RecompensasCanjeUiState.Lista)
    }

    @Test fun falloAlLeerPreferenciasNoOcultaLaTienda() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.canjeadasResultado = Resultado.Exito(emptyList())
        preferences.resultado = Resultado.Fallo(ErrorApp.Desconocido)

        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()

        val state = vm.estado.value as RecompensasCanjeUiState.Lista
        assertEquals(listOf(reward), state.recompensas)
        assertEquals(emptyList<RedeemedReward>(), state.canjeadas)
    }

    @Test fun alAbrirLaTienda_reanudaUnCanjePendiente() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        val pending = PendingRedemption(
            redemptionId = "r-pending",
            rewardId = reward.id,
            name = reward.name,
            costPoints = reward.costPoints,
            code = "DC-cg",
            pointsDebited = 20,
            lotDebits = mapOf("lot-1" to 20),
            createdAt = Instant.now(),
        )
        points.pendienteResultado = Resultado.Exito(pending)
        val redeemed = RedeemedReward("r-pending", reward.id, reward.name, reward.costPoints, "redeem-r-pending-lot-1", "DC-cg", Instant.now())
        points.reanudarResultado = Resultado.Exito(redeemed)

        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()

        assertEquals(RecompensasCanjeUiState.CanjeExitoso(redeemed), vm.estado.value)
        assertEquals(listOf("ana"), points.llamadasReanudar)
    }

    @Test fun alReanudarCajaPendiente_agregaElPremioAlInventarioSinDuplicarlo() = runTest {
        val box = Recompensa(
            id = "surprise-box", name = "Caja sorpresa", description = "Cosmético de la app.",
            costPoints = 120, kind = TipoRecompensa.CAJA_SORPRESA, order = 1,
        )
        catalog.resultadoRecompensas = Resultado.Exito(listOf(box))
        points.pendienteResultado = Resultado.Exito(PendingRedemption(
            redemptionId = "box-1", rewardId = box.id, name = box.name, costPoints = box.costPoints,
            code = null, pointsDebited = 60, lotDebits = mapOf("lot-1" to 60), createdAt = Instant.now(),
            kind = TipoRecompensa.CAJA_SORPRESA, config = mapOf("eligibility" to "unowned-cosmetics"),
            grantedRewardId = "background-montanas", grantedRewardName = "Fondo Montañas",
            grantedRewardKind = TipoRecompensa.FONDO_ENFOQUE,
            grantedRewardConfig = mapOf("background" to "mountains"),
        ))
        val redeemed = RedeemedReward(
            redemptionId = "box-1", rewardId = box.id, name = box.name, costPoints = box.costPoints,
            movementId = "movement-1", code = null, createdAt = Instant.now(),
            kind = TipoRecompensa.CAJA_SORPRESA, config = mapOf("eligibility" to "unowned-cosmetics"),
            grantedRewardId = "background-montanas", grantedRewardName = "Fondo Montañas",
            grantedRewardKind = TipoRecompensa.FONDO_ENFOQUE,
            grantedRewardConfig = mapOf("background" to "mountains"),
        )
        points.reanudarResultado = Resultado.Exito(redeemed)

        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()

        assertEquals(listOf("background-montanas"), vm.propiedad.value.map { it.rewardId })
        assertEquals(1, vm.propiedad.value.size)
    }

    @Test fun canjearDelegaElIdYLaRecompensa() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.saldoResultado = Resultado.Exito(50)
        val redeemed = RedeemedReward("r1", reward.id, reward.name, reward.costPoints, "redeem-r1", "DC-cjE", Instant.now())
        points.canjeResultado = Resultado.Exito(redeemed)
        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()
        vm.canjear(reward, "r1")
        advanceUntilIdle()
        assertEquals("r1", points.ultimoCanje?.third)
        assertTrue(vm.estado.value is RecompensasCanjeUiState.CanjeExitoso)
    }

    @Test fun saldoInsuficienteEmiteFeedbackYNoIntentaCanjear() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.saldoResultado = Resultado.Exito(0)
        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()
        val feedback = async { vm.feedback.first() }
        vm.canjear(reward, "r2")
        advanceUntilIdle()
        assertEquals(CanjeFeedback.SaldoInsuficiente, feedback.await())
        assertEquals(null, points.ultimoCanje)
    }

    @Test fun soloActivaPropiedadPoseida_yCambiarYQuitarPersistenLaSeleccion() = runTest {
        val owned = CosmeticOwnership(
            rewardId = "theme-bosque",
            name = "Tema Bosque",
            kind = TipoRecompensa.TEMA,
            config = mapOf("palette" to "forest"),
            acquiredAt = Instant.now(),
            redemptionId = "purchase-1",
        )
        val second = owned.copy(
            rewardId = "theme-oceano",
            name = "Tema Océano",
            config = mapOf("palette" to "ocean"),
            redemptionId = "purchase-2",
        )
        points.canjeadasResultado = Resultado.Exito(listOf(
            RedeemedReward(
                redemptionId = "purchase-1", rewardId = owned.rewardId, name = owned.name,
                costPoints = 100, movementId = "movement-1", code = null, createdAt = owned.acquiredAt,
                kind = owned.kind, config = owned.config,
            ),
            RedeemedReward(
                redemptionId = "purchase-2", rewardId = second.rewardId, name = second.name,
                costPoints = 150, movementId = "movement-2", code = null, createdAt = owned.acquiredAt,
                kind = second.kind, config = second.config,
            ),
        ))
        catalog.resultadoRecompensas = Resultado.Exito(emptyList())
        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()

        assertEquals(listOf(owned, second), vm.propiedad.value)
        vm.activarCosmetico(owned.rewardId)
        advanceUntilIdle()
        vm.activarCosmetico(second.rewardId)
        advanceUntilIdle()
        assertEquals(second.rewardId, vm.preferencias.value.activeCosmetics[TipoRecompensa.TEMA])
        vm.quitarCosmetico(TipoRecompensa.TEMA)
        advanceUntilIdle()
        assertEquals(null, vm.preferencias.value.activeCosmetics[TipoRecompensa.TEMA])
        assertEquals(listOf(
            Triple("ana", TipoRecompensa.TEMA, owned.rewardId),
            Triple("ana", TipoRecompensa.TEMA, second.rewardId),
            Triple("ana", TipoRecompensa.TEMA, null),
        ), preferences.selecciones)
    }

    @Test fun rechazaAplicarUnCosmeticoNoPoseido() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(emptyList())
        points.canjeadasResultado = Resultado.Exito(emptyList())
        val vm = RecompensasCanjeViewModel("ana", catalog, points, preferences, connectivity)
        advanceUntilIdle()

        vm.activarCosmetico("theme-bosque")
        advanceUntilIdle()

        assertEquals(emptyList<Triple<String, TipoRecompensa, String?>>(), preferences.selecciones)
        assertEquals(emptyMap<TipoRecompensa, String>(), vm.preferencias.value.activeCosmetics)
    }
}
