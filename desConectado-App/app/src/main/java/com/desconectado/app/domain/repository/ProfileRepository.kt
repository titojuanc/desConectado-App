package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado

/** Perfil de la persona, guardado en `users/{uid}`. */
interface ProfileRepository {
    /** Lee el perfil desde el servidor, nunca desde una caché (FR-011). */
    suspend fun perfil(uid: String): Resultado<Perfil>

    /** Crea el perfil si falta; `nombre` puede ser nulo (cuenta de Google sin nombre). */
    suspend fun asegurarPerfil(uid: String, nombre: String?, email: String): Resultado<Unit>
}
