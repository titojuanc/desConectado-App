package com.desconectado.app.data.catalog

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.model.Desafio
import com.desconectado.app.domain.model.Dificultad
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.repository.CatalogRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Catálogos de solo lectura en `challenges` y `rewards`. Siempre se lee desde el servidor: sin
 * conexión devuelven `SinConexion` y nunca datos de una caché (FR-011).
 */
class FirestoreCatalogRepository(private val firestore: FirebaseFirestore) : CatalogRepository {

    override suspend fun desafios(): Resultado<List<Desafio>> = try {
        val consulta = firestore.collection("challenges").orderBy("order", Query.Direction.ASCENDING)
        val documentos = consulta.get(Source.SERVER).await().documents
        Resultado.Exito(documentos.mapNotNull { it.aDesafio() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun recompensas(): Resultado<List<Recompensa>> = try {
        val consulta = firestore.collection("rewards").orderBy("order", Query.Direction.ASCENDING)
        val documentos = consulta.get(Source.SERVER).await().documents
        Resultado.Exito(documentos.mapNotNull { it.aRecompensa() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    private fun DocumentSnapshot.aRecompensa(): Recompensa? {
        val nombre = getString("name") ?: return null
        val descripcion = getString("description") ?: return null
        val costo = getLong("costPoints")?.toInt() ?: return null
        val tipo = TipoRecompensa.desdeAlmacen(getString("kind")) ?: return null
        val orden = getLong("order")?.toInt() ?: return null
        return Recompensa(
            id = id,
            name = nombre,
            description = descripcion,
            costPoints = costo,
            kind = tipo,
            order = orden,
        )
    }

    /** Un documento incompleto o con valores fuera de rango se descarta en vez de romper la lista. */
    private fun DocumentSnapshot.aDesafio(): Desafio? {
        val titulo = getString("title") ?: return null
        val descripcion = getString("description") ?: return null
        val duracion = getLong("durationMinutes")?.toInt() ?: return null
        val dificultad = Dificultad.desdeAlmacen(getString("difficulty")) ?: return null
        val puntos = getLong("points")?.toInt() ?: return null
        val orden = getLong("order")?.toInt() ?: return null
        return Desafio(
            id = id,
            title = titulo,
            description = descripcion,
            durationMinutes = duracion,
            difficulty = dificultad,
            points = puntos,
            order = orden,
        )
    }
}
