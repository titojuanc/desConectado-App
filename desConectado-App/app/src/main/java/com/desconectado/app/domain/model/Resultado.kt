package com.desconectado.app.domain.model

/** Resultado de una operación de red: éxito con un valor, o un error tipado (contracts/repositories.md). */
sealed interface Resultado<out T> {
    data class Exito<out T>(val valor: T) : Resultado<T>
    data class Fallo(val error: ErrorApp) : Resultado<Nothing>
}

/** Errores que la interfaz sabe traducir a un mensaje en español; nunca se muestra el texto crudo del servicio. */
sealed interface ErrorApp {
    /** No hay red o falló el acceso al servicio por red (FR-011). */
    data object SinConexion : ErrorApp

    /** Registro con un correo ya registrado (FR-003). */
    data object CorreoEnUso : ErrorApp

    /** Correo no registrado o contraseña incorrecta; no se distingue (FR-004). */
    data object CredencialesInvalidas : ErrorApp

    /** La persona cerró la pantalla de Google. */
    data object Cancelado : ErrorApp

    /** Google con un correo que ya tiene contraseña y el servicio no unifica solo (FR-008). */
    data object CuentaExistenteConOtroProveedor : ErrorApp

    /** Cualquier otro fallo. */
    data object Desconocido : ErrorApp
}

/** El error si es un fallo, o `null` si fue un éxito. */
fun Resultado<*>.errorOrNull(): ErrorApp? = (this as? Resultado.Fallo)?.error

/** El valor si fue un éxito, o `null` si fue un fallo. */
fun <T> Resultado<T>.valorOrNull(): T? = (this as? Resultado.Exito<T>)?.valor
