package com.desconectado.app.ui.rewards

import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.RedeemedReward
import com.desconectado.app.fakes.FakeCatalogRepository
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
    private val connectivity = FakeConnectivityMonitor()
    private val reward = Recompensa("coupon", "Cupón", "Digital", 50, TipoRecompensa.CUPON, 1)

    @Test fun cargaCatalogoYCanjeadas() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.canjeadasResultado = Resultado.Exito(emptyList())
        val vm = RecompensasCanjeViewModel("ana", catalog, points, connectivity)
        advanceUntilIdle()
        assertTrue(vm.estado.value is RecompensasCanjeUiState.Lista)
    }

    @Test fun canjearDelegaElIdYLaRecompensa() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.saldoResultado = Resultado.Exito(50)
        val redeemed = RedeemedReward("r1", reward.id, reward.name, reward.costPoints, "redeem-r1", "DC-cjE", Instant.now())
        points.canjeResultado = Resultado.Exito(redeemed)
        val vm = RecompensasCanjeViewModel("ana", catalog, points, connectivity)
        advanceUntilIdle()
        vm.canjear(reward, "r1")
        advanceUntilIdle()
        assertEquals("r1", points.ultimoCanje?.third)
        assertTrue(vm.estado.value is RecompensasCanjeUiState.CanjeExitoso)
    }

    @Test fun saldoInsuficienteEmiteFeedbackYNoIntentaCanjear() = runTest {
        catalog.resultadoRecompensas = Resultado.Exito(listOf(reward))
        points.saldoResultado = Resultado.Exito(0)
        val vm = RecompensasCanjeViewModel("ana", catalog, points, connectivity)
        advanceUntilIdle()
        val feedback = async { vm.feedback.first() }
        vm.canjear(reward, "r2")
        advanceUntilIdle()
        assertEquals(CanjeFeedback.SaldoInsuficiente, feedback.await())
        assertEquals(null, points.ultimoCanje)
    }
}
