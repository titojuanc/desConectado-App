package com.desconectado.app.domain

import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.TipoRecompensa

fun TipoRecompensa.esCosmetico(): Boolean = when (this) {
    TipoRecompensa.TEMA,
    TipoRecompensa.FONDO_ENFOQUE,
    TipoRecompensa.PACK_ICONOS,
    TipoRecompensa.MARCO_PERFIL,
    TipoRecompensa.ICONO_PUNTOS,
    TipoRecompensa.ANIMACION_COMPLETADO,
    TipoRecompensa.SONIDO_COMPLETADO -> true
    TipoRecompensa.INSIGNIA,
    TipoRecompensa.CUPON,
    TipoRecompensa.CAJA_SORPRESA -> false
}

fun seleccionarPremioSorpresa(
    recompensas: List<Recompensa>,
    ownedRewardIds: Set<String>,
    redemptionId: String,
): Recompensa? {
    val elegibles = recompensas
        .filter { it.active && it.kind.esCosmetico() && it.id !in ownedRewardIds }
        .sortedWith(compareBy<Recompensa> { it.order }.thenBy { it.id })
    if (elegibles.isEmpty()) return null
    return elegibles[Math.floorMod(redemptionId.hashCode(), elegibles.size)]
}