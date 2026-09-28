package com.desconectado.app.data.points

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.model.DesafioHecho
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.PointsRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Lee los movimientos de `users/{uid}/movements` (contracts/firestore-data.md). La consulta filtra
 * por `type` y ordena por `createdAt`, por eso necesita el índice de `firestore.indexes.json`.
 * Siempre lee del servidor (research D-7).
 */
class FirestorePointsRepository(private val firestore: FirebaseFirestore) : PointsRepository {

    override suspend fun ultimosDesafiosHechos(uid: String, limite: Int): Resultado<List<DesafioHecho>> = try {
        val consulta = firestore.collection("users").document(uid).collection("movements")
            .whereEqualTo("type", TIPO_ACREDITACION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limite.toLong())
        val documentos = consulta.get(Source.SERVER).await().documents
        Resultado.Exito(documentos.mapNotNull { it.aDesafioHecho() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    /** Un movimiento incompleto o con valores fuera de rango se descarta en vez de romper la lista. */
    private fun DocumentSnapshot.aDesafioHecho(): DesafioHecho? {
        val titulo = getString("challengeTitle")?.takeIf { it.isNotBlank() } ?: return null
        val monto = getLong("amount")?.takeIf { it in 1..Int.MAX_VALUE } ?: return null
        val fecha = getTimestamp("createdAt")?.toDate()?.toInstant() ?: return null
        return DesafioHecho(titulo = titulo, puntos = monto.toInt(), fecha = fecha)
    }

    private companion object {
        const val TIPO_ACREDITACION = "credit"
    }
}
