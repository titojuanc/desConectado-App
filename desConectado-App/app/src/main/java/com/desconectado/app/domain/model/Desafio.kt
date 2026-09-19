package com.desconectado.app.domain.model

/** Dificultad de un desafío, de menor a mayor. `valorAlmacen` es el valor guardado en Firestore. */
enum class Dificultad(val valorAlmacen: String) {
    FACIL("easy"),
    NORMAL("normal"),
    DIFICIL("hard"),
    ;

    companion object {
        fun desdeAlmacen(valor: String?): Dificultad? = entries.firstOrNull { it.valorAlmacen == valor }
    }
}

/**
 * Propuesta predefinida "No uses redes sociales por X tiempo", provista por la plataforma.
 *
 * `durationMinutes` y `points` son mayores que 0.
 */
data class Desafio(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val difficulty: Dificultad,
    val points: Int,
    val order: Int,
)
