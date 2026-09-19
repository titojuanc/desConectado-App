package com.desconectado.app.domain

/** Motivo por el que un campo de formulario es inválido. La interfaz lo traduce a un mensaje. */
enum class ErrorCampo {
    VACIO,
    FORMATO,
    MUY_CORTO,
    MUY_LARGO,
}

/** Campos inválidos del registro, cada uno con su motivo; `null` significa que el campo es válido. */
data class ErroresRegistro(
    val username: ErrorCampo? = null,
    val email: ErrorCampo? = null,
    val password: ErrorCampo? = null,
) {
    val esValido: Boolean get() = username == null && email == null && password == null
}

const val USERNAME_MIN = 3
const val USERNAME_MAX = 30
const val PASSWORD_MIN = 8

private val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Recorta espacios y pasa a minúsculas (FR-009). */
fun normalizarCorreo(correo: String): String = correo.trim().lowercase()

/** Formato de correo válido tras normalizar (FR-002). */
fun validarCorreo(correo: String): ErrorCampo? {
    val normalizado = normalizarCorreo(correo)
    return when {
        normalizado.isEmpty() -> ErrorCampo.VACIO
        !FORMATO_CORREO.matches(normalizado) -> ErrorCampo.FORMATO
        else -> null
    }
}

/** Al menos 8 caracteres; los espacios y caracteres especiales cuentan tal cual, sin recortar (FR-002). */
fun validarPassword(password: String): ErrorCampo? = when {
    password.isEmpty() -> ErrorCampo.VACIO
    password.length < PASSWORD_MIN -> ErrorCampo.MUY_CORTO
    else -> null
}

/** Entre 3 y 30 caracteres tras recortar (FR-002). */
fun validarUsername(username: String): ErrorCampo? {
    val recortado = username.trim()
    return when {
        recortado.isEmpty() -> ErrorCampo.VACIO
        recortado.length < USERNAME_MIN -> ErrorCampo.MUY_CORTO
        recortado.length > USERNAME_MAX -> ErrorCampo.MUY_LARGO
        else -> null
    }
}

/** Valida los tres campos a la vez para poder mostrar todos los errores juntos (FR-002). */
fun validarRegistro(username: String, email: String, password: String): ErroresRegistro =
    ErroresRegistro(
        username = validarUsername(username),
        email = validarCorreo(email),
        password = validarPassword(password),
    )

/**
 * Nombre de usuario para el perfil de una cuenta que no pasó por el formulario de registro: el
 * nombre de Google si viene y no está en blanco; si no, la parte local del correo. Siempre
 * recortado y de hasta 30 caracteres, que es lo que acepta el servidor (data-model.md).
 */
fun nombreParaPerfil(nombre: String?, email: String): String {
    val base = nombre?.trim().takeUnless { it.isNullOrEmpty() }
        ?: email.trim().substringBefore('@')
    return base.trim().take(USERNAME_MAX).trimEnd()
}
