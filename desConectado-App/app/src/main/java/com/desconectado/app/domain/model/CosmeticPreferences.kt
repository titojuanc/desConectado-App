package com.desconectado.app.domain.model

data class CosmeticPreferences(
    val activeCosmetics: Map<TipoRecompensa, String> = emptyMap(),
)