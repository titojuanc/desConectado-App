package com.desconectado.app.data.profile

import com.desconectado.app.data.aErrorApp
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.nombreParaPerfil
import com.desconectado.app.domain.normalizarCorreo
import com.desconectado.app.domain.repository.ProfileRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Perfil guardado en `users/{uid}` (contracts/firestore-data.md). */
class FirestoreProfileRepository(private val firestore: FirebaseFirestore) : ProfileRepository {

    private fun documento(uid: String): DocumentReference = firestore.collection("users").document(uid)

    override suspend fun perfil(uid: String): Resultado<Perfil> = try {
        val doc = documento(uid).get(Source.SERVER).await()
        val username = doc.getString("username")
        val email = doc.getString("email")
        if (doc.exists() && username != null && email != null) {
            Resultado.Exito(Perfil(username = username, email = email))
        } else {
            Resultado.Fallo(ErrorApp.Desconocido)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    override suspend fun asegurarPerfil(uid: String, nombre: String?, email: String): Resultado<Unit> = try {
        val ref = documento(uid)
        if (!ref.get(Source.SERVER).await().exists()) {
            crear(ref, nombre, email)
        }
        Resultado.Exito(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resultado.Fallo(e.aErrorApp())
    }

    private suspend fun crear(ref: DocumentReference, nombre: String?, email: String) {
        val correo = normalizarCorreo(email)
        val datos = mapOf(
            "username" to nombreParaPerfil(nombre, correo),
            "email" to correo,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        try {
            ref.set(datos).await()
        } catch (e: FirebaseFirestoreException) {
            // Dos llamadas casi simultáneas: si la otra ya creó el perfil, las reglas rechazan la
            // segunda (el perfil no se puede modificar) y el objetivo ya está cumplido.
            val yaExiste = e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                ref.get(Source.SERVER).await().exists()
            if (!yaExiste) throw e
        }
    }
}
