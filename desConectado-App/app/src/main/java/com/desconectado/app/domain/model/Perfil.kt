package com.desconectado.app.domain.model

/**
 * Datos de la persona que se muestran en el perfil. Nunca incluye la contraseña (FR-010, FR-021).
 *
 * - `username`: al registrarse, de 3 a 30 caracteres, recortado; el servidor acepta de 1 a 30.
 * - `email`: en minúsculas y sin espacios; igual al correo de la cuenta autenticada.
 * - `puntos`: saldo de puntos (`pointsBalance`); vale 0 en una cuenta nueva y nunca es negativo
 *   (FR-027, FR-030, FR-031). Solo se lee: la app no lo modifica (FR-032).
 */
data class Perfil(
    val username: String,
    val email: String,
    val puntos: Int = 0,
)
