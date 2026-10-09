package com.desconectado.app.data.profile

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.esCosmetico
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.repository.CosmeticPreferencesRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

class FirestoreCosmeticPreferencesRepository(private val firestore: FirebaseFirestore) : CosmeticPreferencesRepository {
    override suspend fun leer(uid: String): Resultado<CosmeticPreferences> = try {
        val snapshot = referencia(uid).get(Source.SERVER).await()
        Resultado.Exito(snapshot.aPreferencias())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun seleccionar(
        uid: String,
        kind: TipoRecompensa,
        rewardId: String?,
    ): Resultado<CosmeticPreferences> = try {
        require(kind.esCosmetico())
        val ref = referencia(uid)
        val updated = firestore.runTransaction { transaction ->
            val current = transaction.get(ref).aPreferencias().activeCosmetics.toMutableMap()
            if (rewardId == null) current.remove(kind) else current[kind] = rewardId
            val next = CosmeticPreferences(current.toMap())
            transaction.set(ref, mapOf("activeCosmetics" to next.aFirestore()), SetOptions.merge())
            next
        }.await()
        Resultado.Exito(updated)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override fun observar(uid: String): Flow<CosmeticPreferences> = callbackFlow {
        val registration = referencia(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.aPreferencias() ?: CosmeticPreferences())
        }
        awaitClose { registration.remove() }
    }

    private fun referencia(uid: String) = firestore.collection("users").document(uid)
        .collection("preferences").document("current")

    private fun DocumentSnapshot.aPreferencias(): CosmeticPreferences {
        val active = (get("activeCosmetics") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val kind = TipoRecompensa.desdeAlmacen(key as? String) ?: return@mapNotNull null
                val id = value as? String ?: return@mapNotNull null
                if (!kind.esCosmetico()) return@mapNotNull null
                kind to id
            }
            ?.toMap()
            ?: emptyMap()
        return CosmeticPreferences(active)
    }

    private fun CosmeticPreferences.aFirestore(): Map<String, String> = activeCosmetics
        .filterKeys { it.esCosmetico() }
        .mapKeys { (kind, _) -> kind.valorAlmacen }
}