package com.desconectado.app.domain

import com.desconectado.app.domain.model.Dificultad

// Estas dos funciones son puras y viven en `domain` para poder probarlas en la JVM sin recursos de
// Android; son la única fuente de estos textos y la interfaz las usa tal cual.

/** Duración legible en español: "30 minutos", "1 hora", "2 horas", "1 hora 30 minutos". */
fun formatearDuracion(minutos: Int): String {
    val horas = minutos / 60
    val resto = minutos % 60
    val partes = buildList {
        if (horas > 0) add(if (horas == 1) "1 hora" else "$horas horas")
        if (resto > 0) add(if (resto == 1) "1 minuto" else "$resto minutos")
    }
    return partes.joinToString(" ").ifEmpty { "0 minutos" }
}

/** Etiqueta de dificultad para mostrar: Fácil, Normal o Difícil. */
fun etiquetaDificultad(dificultad: Dificultad): String = when (dificultad) {
    Dificultad.FACIL -> "Fácil"
    Dificultad.NORMAL -> "Normal"
    Dificultad.DIFICIL -> "Difícil"
}
