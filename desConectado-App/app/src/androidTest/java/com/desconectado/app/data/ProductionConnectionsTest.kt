package com.desconectado.app.data

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.desconectado.app.BuildConfig
import com.desconectado.app.DesConectadoApp
import com.desconectado.app.domain.model.ChallengeRating
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProductionConnectionsTest {
    @Test
    fun repositoriesConnectToProductionWithIsolatedAccount() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("productionSmoke") == "des-conectado")
        assumeTrue(!BuildConfig.USE_FIREBASE_EMULATOR)
        val app = ApplicationProvider.getApplicationContext<Context>() as DesConectadoApp
        val container = app.container
        assertEquals("des-conectado", container.auth.app.options.projectId)
        assertNull("Sign out of existing accounts before running production smoke", container.auth.currentUser)
        val email = "android-smoke-${UUID.randomUUID()}@example.invalid"
        val password = UUID.randomUUID().toString()
        try {
            withTimeout(180_000) {
                success("register", container.authRepository.registrar("Android Smoke QA", email, password, 120))
                val uid = requireNotNull(container.auth.currentUser).uid
                Log.i("ProductionSmoke", "QA_UID=$uid")
                container.authRepository.cerrarSesion()
                success("sign in", container.authRepository.ingresar(email, password))
                assertEquals(uid, container.auth.currentUser?.uid)
                container.authRepository.verificarCuenta()
                assertEquals(uid, container.auth.currentUser?.uid)

                assertEquals(0, success("profile", container.perfilRepository.perfil(uid)).puntos)
                success("username update", container.perfilRepository.actualizarUsername(uid, "Android Smoke Updated"))
                assertEquals("Android Smoke Updated", success("profile reload", container.perfilRepository.perfil(uid)).username)
                val preferences = success("preferences", container.userPreferencesRepository.leer(uid))
                assertEquals(120, preferences.weeklyGoalMinutes)
                assertFalse(preferences.notificationsEnabled)
                success("weekly goal update", container.userPreferencesRepository.guardarMetaSemanal(uid, 180))
                success("notifications on", container.userPreferencesRepository.configurarNotificaciones(uid, true))
                assertTrue(success("preferences reload", container.userPreferencesRepository.leer(uid)).notificationsEnabled)
                success("notifications off", container.userPreferencesRepository.configurarNotificaciones(uid, false))
                assertEquals(180, success("weekly goal reload", container.userPreferencesRepository.leer(uid)).weeklyGoalMinutes)

                val challenges = success("challenge catalog", container.catalogRepository.desafios())
                val rewards = success("reward catalog", container.catalogRepository.recompensas())
                assertEquals(28, challenges.size)
                assertEquals(17, rewards.size)
                assertEquals(0, success("balance and expiration query", container.pointsRepository.saldo(uid)))
                assertNull(success("next expiration", container.pointsRepository.proximoVencimiento(uid)))
                assertTrue(success("movement history", container.pointsRepository.ultimosDesafiosHechos(uid)).isEmpty())
                assertTrue(success("redemption history", container.pointsRepository.recompensasCanjeadas(uid)).isEmpty())
                assertNull(success("pending redemption", container.pointsRepository.pendingRedemption(uid)))
                assertNull(success("resume redemption", container.pointsRepository.resumePendingRedemption(uid)))
                assertTrue(success("challenge history", container.challengeRepository.history(uid)).isEmpty())
                val results = success("challenge results", container.challengeRepository.results(uid))
                assertTrue(results.isEmpty())
                assertNull(success("active challenge", container.challengeRepository.active(uid)))
                assertEquals(10, success("achievement definitions and progress", container.achievementRepository.actualizar(uid, results)).size)
                success("cosmetic preferences", container.cosmeticPreferencesRepository.leer(uid))
                success("remove unselected theme", container.cosmeticPreferencesRepository.seleccionar(uid, TipoRecompensa.TEMA, null))
                assertNotNull(success("cosmetic preferences reload", container.cosmeticPreferencesRepository.leer(uid)))
                val theme = rewards.first { it.kind == TipoRecompensa.TEMA }
                rejected("unowned theme", container.cosmeticPreferencesRepository.seleccionar(uid, theme.kind, theme.id))
                assertTrue(success("unchanged cosmetics", container.cosmeticPreferencesRepository.leer(uid)).activeCosmetics.isEmpty())
                rejected("insufficient balance redemption", container.pointsRepository.redeem(uid, theme, UUID.randomUUID().toString()))
                assertEquals(0, success("balance unchanged", container.pointsRepository.saldo(uid)))
                assertNull(success("no pending debit", container.pointsRepository.pendingRedemption(uid)))

                success("start challenge", container.challengeRepository.start(uid, challenges.first().id))
                assertNotNull(success("active challenge persisted", container.challengeRepository.active(uid)))
                val runId = UUID.randomUUID().toString()
                val cancelled = success("cancel challenge", container.challengeRepository.cancel(uid, runId))
                assertEquals(ChallengeResult.Status.CANCELLED, cancelled.status)
                assertEquals(0, cancelled.pointsAwarded)
                assertNull(success("active challenge cleared", container.challengeRepository.active(uid)))
                success("idempotent cancellation", container.challengeRepository.cancel(uid, runId))
                assertEquals(1, success("cancelled result persisted", container.challengeRepository.results(uid)).size)
                assertEquals(1, success("cancelled history persisted", container.challengeRepository.history(uid)).size)
                rejected("rating cancelled challenge", container.challengeRepository.rate(uid, ChallengeRating(runId, 5)))
                assertNull(success("no cancelled rating", container.challengeRepository.rating(uid, runId)))
                assertEquals(0, success("cancel does not credit", container.pointsRepository.saldo(uid)))

                if (InstrumentationRegistry.getArguments().getString("syntheticQa") != "true") return@withTimeout
                val challenge = challenges.maxBy { it.points }
                assertTrue(challenge.points >= theme.costPoints)
                val active = success("start synthetic QA challenge", container.challengeRepository.start(uid, challenge.id))
                val finishedAt = Instant.now()
                val synthetic = ChallengeResult(
                    challengeRunId = UUID.randomUUID().toString(),
                    challengeId = active.challengeId,
                    challengeTitle = active.challengeTitle,
                    durationMinutes = active.durationMinutes,
                    durationSeconds = active.durationSeconds,
                    startedAt = finishedAt.minusSeconds(active.durationSeconds.toLong() + 60),
                    finishedAt = finishedAt,
                    status = ChallengeResult.Status.COMPLETED,
                    measuredSocialSeconds = 0,
                    offlineSeconds = 0,
                    pointsAwarded = active.points,
                    category = active.category,
                )
                val completed = success("finish synthetic QA result", container.challengeRepository.finish(uid, synthetic.challengeRunId, synthetic))
                success("credit completed fixture", container.pointsRepository.acreditar(uid, completed))
                success("idempotent credit", container.pointsRepository.acreditar(uid, completed))
                assertEquals(challenge.points, success("credited balance", container.pointsRepository.saldo(uid)))
                assertNotNull(success("funded expiration", container.pointsRepository.proximoVencimiento(uid)))
                assertEquals(1, success("single credit movement", container.pointsRepository.ultimosDesafiosHechos(uid)).size)
                val rating = ChallengeRating(completed.challengeRunId, 4)
                success("completed rating write", container.challengeRepository.rate(uid, rating))
                success("idempotent rating", container.challengeRepository.rate(uid, rating))
                assertEquals(4, success("rating persisted", container.challengeRepository.rating(uid, completed.challengeRunId))?.stars)
                val completedResults = success("completed results persisted", container.challengeRepository.results(uid))
                val progress = success("achievement progress writes", container.achievementRepository.actualizar(uid, completedResults))
                assertTrue(progress.any { it.progress > 0 })
                success("idempotent achievement update", container.achievementRepository.actualizar(uid, completedResults))

                val redemptionId = UUID.randomUUID().toString()
                success("theme redemption", container.pointsRepository.redeem(uid, theme, redemptionId))
                success("idempotent theme redemption", container.pointsRepository.redeem(uid, theme, redemptionId))
                assertEquals(challenge.points - theme.costPoints, success("debited balance", container.pointsRepository.saldo(uid)))
                assertEquals(1, success("single redemption", container.pointsRepository.recompensasCanjeadas(uid)).size)
                assertNull(success("pending cleared after redemption", container.pointsRepository.pendingRedemption(uid)))
                success("apply owned theme", container.cosmeticPreferencesRepository.seleccionar(uid, theme.kind, theme.id))
                assertEquals(theme.id, success("owned theme persisted", container.cosmeticPreferencesRepository.leer(uid)).activeCosmetics[theme.kind])
                success("remove owned theme", container.cosmeticPreferencesRepository.seleccionar(uid, theme.kind, null))
                assertTrue(success("theme removed persisted", container.cosmeticPreferencesRepository.leer(uid)).activeCosmetics.isEmpty())

                val additional = synthetic.copy(challengeRunId = UUID.randomUUID().toString())
                success("start second QA fixture", container.challengeRepository.start(uid, challenge.id))
                success("finish second QA fixture", container.challengeRepository.finish(uid, additional.challengeRunId, additional))
                success("credit second QA fixture", container.pointsRepository.acreditar(uid, additional))
                val box = rewards.first { it.kind == TipoRecompensa.CAJA_SORPRESA }
                val boxId = UUID.randomUUID().toString()
                val granted = success("surprise box redemption", container.pointsRepository.redeem(uid, box, boxId))
                assertNotNull(granted.grantedRewardId)
                val repeatedGrant = success("idempotent surprise box", container.pointsRepository.redeem(uid, box, boxId))
                assertEquals(granted.grantedRewardId, repeatedGrant.grantedRewardId)
                val grantedReward = rewards.first { it.id == granted.grantedRewardId }
                success("apply box-owned cosmetic", container.cosmeticPreferencesRepository.seleccionar(uid, grantedReward.kind, grantedReward.id))
                assertEquals(grantedReward.id, success("box ownership persisted", container.cosmeticPreferencesRepository.leer(uid)).activeCosmetics[grantedReward.kind])
                val balance = challenge.points * 2 - theme.costPoints - box.costPoints
                assertEquals(balance, success("box debited once", container.pointsRepository.saldo(uid)))
                assertEquals(2, success("both canjes persisted", container.pointsRepository.recompensasCanjeadas(uid)).size)

                val aged = synthetic.copy(
                    challengeRunId = UUID.randomUUID().toString(),
                    startedAt = synthetic.startedAt.minusSeconds(35L * 24 * 60 * 60),
                    finishedAt = synthetic.finishedAt.minusSeconds(35L * 24 * 60 * 60),
                )
                success("start aged QA fixture", container.challengeRepository.start(uid, challenge.id))
                success("finish aged QA fixture", container.challengeRepository.finish(uid, aged.challengeRunId, aged))
                success("credit aged QA fixture", container.pointsRepository.acreditar(uid, aged))
                assertEquals(balance, success("expired lot debited", container.pointsRepository.saldo(uid)))
                assertEquals(balance, success("idempotent expiration", container.pointsRepository.saldo(uid)))
            }
        } finally {
            container.authRepository.cerrarSesion()
        }
    }

    private fun <Value> success(step: String, result: Resultado<Value>): Value {
        assertTrue("$step failed: $result", result is Resultado.Exito)
        Log.i("ProductionSmoke", "PASS: $step")
        return (result as Resultado.Exito).valor
    }

    private fun rejected(step: String, result: Resultado<*>) {
        assertTrue("$step unexpectedly succeeded", result is Resultado.Fallo)
        Log.i("ProductionSmoke", "PASS: $step rejected")
    }
}