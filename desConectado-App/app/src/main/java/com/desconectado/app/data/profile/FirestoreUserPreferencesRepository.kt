package com.desconectado.app.data.profile

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.repository.UserPreferencesRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserPreferencesRepository(private val firestore: FirebaseFirestore) : UserPreferencesRepository {
    override suspend fun leer(uid: String): Resultado<UserPreferences> = try {
        Resultado.Exito(referencia(uid).get(Source.SERVER).await().aUserPreferences())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override fun observar(uid: String): Flow<UserPreferences> = callbackFlow {
        val registration = referencia(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.aUserPreferences() ?: UserPreferences())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun guardarMetaSemanal(uid: String, minutes: Int): Resultado<UserPreferences> =
        actualizar(uid) { it.copy(weeklyGoalMinutes = minutes) }

    override suspend fun configurarNotificaciones(uid: String, enabled: Boolean): Resultado<UserPreferences> =
        actualizar(uid) { it.copy(notificationsEnabled = enabled) }

    private suspend fun actualizar(
        uid: String,
        transform: (UserPreferences) -> UserPreferences,
    ): Resultado<UserPreferences> = try {
        val ref = referencia(uid)
        val result = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).aUserPreferences()
            val updated = transform(current)
            transaction.set(ref, updated.toFirestoreMap(), SetOptions.merge())
            updated
        }.await()
        Resultado.Exito(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    private fun referencia(uid: String): DocumentReference = firestore.collection("users").document(uid)
        .collection("preferences").document("current")

    private fun DocumentSnapshot.aUserPreferences(): UserPreferences {
        val weeklyGoal = getLong("weeklyGoalMinutes")?.toInt()?.takeIf {
            it in UserPreferences.META_MINIMA..UserPreferences.META_MAXIMA && it % UserPreferences.INCREMENTO_META == 0
        }
        val active = CosmeticPreferences(
            activeCosmetics = (get("activeCosmetics") as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    val kind = TipoRecompensa.desdeAlmacen(key as? String) ?: return@mapNotNull null
                    val id = value as? String ?: return@mapNotNull null
                    kind to id
                }
                ?.toMap()
                ?: emptyMap(),
        )
        return UserPreferences(
            weeklyGoalMinutes = weeklyGoal,
            notificationsEnabled = getBoolean("notificationsEnabled") ?: false,
            activeCosmetics = active.activeCosmetics,
        )
    }

    private fun UserPreferences.toFirestoreMap(): Map<String, Any> = buildMap {
        put("activeCosmetics", activeCosmetics.mapKeys { (kind, _) -> kind.valorAlmacen })
        put("notificationsEnabled", notificationsEnabled)
        weeklyGoalMinutes?.let { put("weeklyGoalMinutes", it) }
    }
}