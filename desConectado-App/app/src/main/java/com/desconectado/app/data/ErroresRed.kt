package com.desconectado.app.data

import com.desconectado.app.domain.model.ErrorApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/** Fragmentos con los que el sistema describe que no se pudo llegar al servidor. */
private val TEXTOS_DE_RED = listOf(
    "failed to connect",
    "unable to resolve host",
    "network is unreachable",
    "connection refused",
    "timed out",
    "timeout",
)

/** `true` si el fallo se debe a que no hay red o no se pudo llegar al servicio (FR-011). */
internal fun Throwable.esErrorDeRed(): Boolean = when (this) {
    is FirebaseNetworkException, is IOException -> true
    is FirebaseFirestoreException ->
        code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    // Con la protección de reCAPTCHA, Auth informa la falta de red como un error interno cuyo texto
    // describe la falla de conexión, en vez de lanzar `FirebaseNetworkException`.
    is FirebaseAuthException -> errorCode == "ERROR_INTERNAL_ERROR" && message.mencionaUnaFallaDeRed()
    else -> cause?.takeIf { it !== this }?.esErrorDeRed() ?: false
}

private fun String?.mencionaUnaFallaDeRed(): Boolean {
    val texto = this?.lowercase() ?: return false
    return TEXTOS_DE_RED.any { it in texto }
}

/** Traducción genérica de un fallo de lectura o escritura: sin red, o desconocido. */
internal fun Throwable.aErrorApp(): ErrorApp = if (esErrorDeRed()) ErrorApp.SinConexion else ErrorApp.Desconocido
