package com.desconectado.app.domain.model

/** Tipo de recompensa. `valorAlmacen` es el valor guardado en Firestore. */
enum class TipoRecompensa(val valorAlmacen: String) {
    INSIGNIA("badge"),
    TEMA("theme"),
    CUPON("coupon"),
    FONDO_ENFOQUE("focus-background"),
    PACK_ICONOS("icon-pack"),
    MARCO_PERFIL("profile-frame"),
    ICONO_PUNTOS("point-icon"),
    ANIMACION_COMPLETADO("completion-animation"),
    SONIDO_COMPLETADO("completion-sound"),
    CAJA_SORPRESA("surprise-box"),
    ;

    companion object {
        fun desdeAlmacen(valor: String?): TipoRecompensa? = entries.firstOrNull { it.valorAlmacen == valor }
    }
}

/**
 * Elemento digital del catálogo de la app, provisto por la plataforma. `costPoints` es mayor que 0.
 * Nunca se presenta como un beneficio canjeable fuera de la app (FR-019).
 */
data class Recompensa(
    val id: String,
    val name: String,
    val description: String,
    val costPoints: Int,
    val kind: TipoRecompensa,
    val order: Int,
    val active: Boolean = true,
    val config: Map<String, String> = emptyMap(),
)
