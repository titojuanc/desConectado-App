package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Resultado
import kotlinx.coroutines.flow.Flow

/** Cuentas y sesión (contracts/repositories.md). */
interface AuthRepository {
    /** Estado de la sesión: `Cargando` hasta conocerlo, luego `SinSesion` o `ConSesion`. */
    val authState: Flow<EstadoSesion>

    /** Crea la cuenta y el perfil, y deja la sesión iniciada. El correo se normaliza (FR-009). */
    suspend fun registrar(username: String, email: String, password: String): Resultado<Unit>

    /** Ingresa con correo y contraseña. El correo se normaliza igual que en `registrar`. */
    suspend fun ingresar(email: String, password: String): Resultado<Unit>

    /** Ingresa con el token de identidad de Google; crea el perfil si no existe (FR-007). */
    suspend fun ingresarConGoogle(idToken: String): Resultado<Unit>

    /** Vincula Google a la cuenta ya autenticada; solo tras `CuentaExistenteConOtroProveedor`. */
    suspend fun vincularGoogle(idToken: String): Resultado<Unit>

    /**
     * Envía el correo de restablecimiento de contraseña en español. Devuelve éxito exista o no una
     * cuenta con ese correo (FR-026).
     */
    suspend fun restablecerPassword(email: String): Resultado<Unit>

    /** Cierra la sesión. Es local y no requiere conexión. */
    fun cerrarSesion()

    /**
     * Recarga la cuenta y cierra la sesión solo si el servicio indica que no existe o fue
     * deshabilitada; un fallo de red NO cierra la sesión.
     */
    suspend fun verificarCuenta()
}
