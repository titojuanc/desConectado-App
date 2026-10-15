package com.desconectado.app.ui.navigation

import com.desconectado.app.AppContainer
import com.desconectado.app.domain.ProgressCalculator
import com.desconectado.app.domain.model.ChallengeResult
import com.desconectado.app.domain.model.Resultado
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

suspend fun ejecutarPruebaRacha(
    container: AppContainer,
    uid: String,
    accion: AccionRachaDebug,
): String {
    check(container.auth.currentUser?.uid == uid) { "La sesión cambió. Volvé a iniciar sesión." }
    check(container.debugSectionPreferenceStore.estaHabilitada()) {
        "Activá la Sección Debug en Perfil antes de ejecutar una prueba."
    }
    check(container.challengeRepository.active(uid).exigirExito() == null) {
        "Terminá o cancelá el desafío activo antes de ejecutar una prueba."
    }
    val goal = checkNotNull(container.userPreferencesRepository.leer(uid).exigirExito().weeklyGoalMinutes) {
        "Configurá una meta semanal en Perfil antes de probar la racha."
    }
    val zona = ZoneId.systemDefault()
    val now = Instant.now()
    val hoy = now.atZone(zona).toLocalDate()
    val user = container.firestore.collection("users").document(uid)
    val historial = container.challengeRepository.results(uid).exigirExito()
    if (accion == AccionRachaDebug.REINICIAR) {
        val recientes = historial.filter {
            it.finishedAt.atZone(ZoneId.of(it.timeZoneId)).toLocalDate() >= hoy.minusDays(2)
        }
        check(recientes.none { !it.challengeRunId.startsWith("debug-streak-") }) {
            "Hay desafíos reales recientes. Usá una cuenta QA sin desafíos reales para reiniciar las pruebas."
        }
        val days = user.collection("dailyStreakProgress").get(Source.SERVER).await().documents
            .filter { it.id >= hoy.minusDays(2).toString() }
        check(days.all { it.getString("lastRunId").orEmpty().startsWith("debug-streak-") }) {
            "No se puede reiniciar el progreso de desafíos reales."
        }
        val refs = recientes.map { user.collection("challengeResults").document(it.challengeRunId) } +
            days.map { it.reference }
        for (chunk in refs.chunked(400)) {
            val batch = container.firestore.batch()
            chunk.forEach(batch::delete)
            batch.commit().await()
        }
        return "Racha de prueba reiniciada. El saldo y los movimientos ya acreditados se conservan."
    }
    val fixture = container.catalogRepository.desafios().exigirExito()
        .firstOrNull { it.id == "debug-10-segundos" && it.active }
    checkNotNull(fixture) { "Falta el desafío debug-10-segundos. Ejecutá seed-debug antes de probar." }
    val metaSegundos = (goal * 60L + 6) / 7
    suspend fun acreditar(segundos: Int, finalizado: Instant): ChallengeResult {
        val resultado = ChallengeResult(
            challengeRunId = "debug-streak-${UUID.randomUUID()}",
            challengeId = fixture.id,
            challengeTitle = "[TEST] Racha y puntos",
            durationMinutes = segundos / 60,
            durationSeconds = segundos,
            startedAt = finalizado.minusSeconds(segundos.toLong()),
            finishedAt = finalizado,
            status = ChallengeResult.Status.COMPLETED,
            measuredSocialSeconds = 0,
            offlineSeconds = 0,
            pointsAwarded = fixture.points,
            timeZoneId = zona.id,
            category = fixture.category,
        )
        val previo = container.challengeRepository.results(uid).exigirExito()
        val guardado = container.challengeRepository.finish(uid, resultado.challengeRunId, resultado).exigirExito()
        return container.pointsRepository.acreditarConRacha(uid, guardado, previo).exigirExito()
    }
    val resultado = when (accion) {
        AccionRachaDebug.ACTIVAR -> {
            for (dias in 2L downTo 1L) {
                val fecha = hoy.minusDays(dias)
                val finalizado = fecha.atTime(12, 0).atZone(zona).toInstant()
                val previos = container.challengeRepository.results(uid).exigirExito()
                val cumplidos = previos.filter {
                    it.status == ChallengeResult.Status.COMPLETED &&
                        it.finishedAt.atZone(ZoneId.of(it.timeZoneId)).toLocalDate() == fecha
                }.sumOf { it.durationSeconds.toLong() }
                if (cumplidos < metaSegundos) acreditar((metaSegundos - cumplidos).toInt(), finalizado)
            }
            null
        }
        AccionRachaDebug.SUMAR_PUNTOS -> acreditar(0, now)
        AccionRachaDebug.COMPLETAR_META -> {
            val dia = user.collection("dailyStreakProgress").document(hoy.toString()).get(Source.SERVER).await()
            val segundos = dia.getLong("completedSeconds") ?: 0
            acreditar((metaSegundos - segundos).coerceAtLeast(0).toInt(), now)
        }
        AccionRachaDebug.REINICIAR -> error("Acción ya procesada")
    }
    val actualizado = container.challengeRepository.results(uid).exigirExito()
    val progreso = ProgressCalculator.calcular(actualizado, Instant.now(), zona, goal)
    val saldo = container.pointsRepository.saldo(uid).exigirExito()
    val credito = resultado?.let { "Crédito: ${it.pointsAwarded} puntos (bonus: ${it.streakBonusPoints}). " }.orEmpty()
    return "${credito}Saldo real: $saldo. Racha: ${progreso.rachaDias} días. " +
        if (progreso.metaDiariaCumplida) "Meta diaria cumplida." else "Meta diaria pendiente: ${progreso.segundosRestantesMetaDiaria} segundos."
}

private fun <T> Resultado<T>.exigirExito(): T = when (this) {
    is Resultado.Exito -> valor
    is Resultado.Fallo -> error("Firebase no pudo completar la prueba: $error")
}
