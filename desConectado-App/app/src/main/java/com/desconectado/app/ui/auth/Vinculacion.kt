package com.desconectado.app.ui.auth

import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.normalizarCorreo
import com.desconectado.app.domain.repository.AuthRepository

/**
 * Google con un correo que ya tiene cuenta de contraseña y Firebase no unificó solo (FR-008): se le
 * pide la contraseña de esa cuenta para vincularle Google.
 */
data class VinculacionPendiente(
    val idToken: String,
    val email: String = "",
    val password: String = "",
    val enviando: Boolean = false,
    val error: ErrorApp? = null,
)

/** Ingresa con la contraseña de la cuenta existente y, si sale bien, le vincula Google. */
internal suspend fun completarVinculacion(auth: AuthRepository, vinculacion: VinculacionPendiente): Resultado<Unit> {
    val ingreso = auth.ingresar(normalizarCorreo(vinculacion.email), vinculacion.password)
    if (ingreso is Resultado.Fallo) return ingreso
    return auth.vincularGoogle(vinculacion.idToken)
}
