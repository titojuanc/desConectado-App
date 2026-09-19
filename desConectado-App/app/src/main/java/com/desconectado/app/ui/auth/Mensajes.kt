package com.desconectado.app.ui.auth

import androidx.annotation.StringRes
import com.desconectado.app.R
import com.desconectado.app.domain.ErrorCampo
import com.desconectado.app.domain.model.ErrorApp

/** Textos de las pantallas de acceso: cada motivo o error se traduce a un recurso en español. */

@StringRes
fun ErrorCampo.mensajeUsername(): Int = when (this) {
    ErrorCampo.VACIO -> R.string.error_username_vacio
    ErrorCampo.MUY_CORTO -> R.string.error_username_corto
    ErrorCampo.MUY_LARGO -> R.string.error_username_largo
    ErrorCampo.FORMATO -> R.string.error_username_vacio
}

@StringRes
fun ErrorCampo.mensajeCorreo(): Int = when (this) {
    ErrorCampo.VACIO -> R.string.error_correo_vacio
    else -> R.string.error_correo_formato
}

@StringRes
fun ErrorCampo.mensajePassword(): Int = when (this) {
    ErrorCampo.VACIO -> R.string.error_password_vacio
    else -> R.string.error_password_corto
}

/** Mensaje de un error de envío, o `null` si la pantalla no debe mostrar ninguno (p. ej. `Cancelado`). */
@StringRes
fun ErrorApp.mensajeEnvio(): Int? = when (this) {
    ErrorApp.CorreoEnUso -> R.string.error_correo_en_uso
    ErrorApp.CredencialesInvalidas -> R.string.error_credenciales
    ErrorApp.SinConexion -> R.string.error_conexion_requerida
    ErrorApp.Cancelado -> null
    ErrorApp.CuentaExistenteConOtroProveedor, ErrorApp.Desconocido -> R.string.error_generico
}
