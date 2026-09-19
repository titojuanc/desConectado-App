package com.desconectado.app.domain.model

/** Estado de la sesión en el dispositivo (data-model.md). No se guarda en la base de datos. */
sealed interface EstadoSesion {
    /** La app está determinando si hay una sesión guardada. */
    data object Cargando : EstadoSesion

    /** No hay persona identificada. */
    data object SinSesion : EstadoSesion

    /** Persona identificada con ese `uid`. */
    data class ConSesion(val uid: String) : EstadoSesion
}

/** Conectividad del dispositivo; bloquea las acciones que usan la red (FR-011). */
enum class Conectividad {
    CONECTADO,
    SIN_CONEXION,
}
