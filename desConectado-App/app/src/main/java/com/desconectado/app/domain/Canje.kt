package com.desconectado.app.domain

import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Recompensa
import com.desconectado.app.domain.model.Resultado
import java.util.Base64

fun validarCanje(saldo: Int, recompensa: Recompensa, yaCanjeada: Boolean): Resultado<Unit> = when {
    recompensa.costPoints <= 0 -> Resultado.Fallo(ErrorApp.Desconocido)
    yaCanjeada && recompensa.kind != com.desconectado.app.domain.model.TipoRecompensa.CUPON -> Resultado.Fallo(ErrorApp.Desconocido)
    saldo < recompensa.costPoints -> Resultado.Fallo(ErrorApp.Desconocido)
    else -> Resultado.Exito(Unit)
}

fun codigoCupon(redemptionId: String): String =
    "DC-" + Base64.getUrlEncoder().withoutPadding().encodeToString(redemptionId.toByteArray(Charsets.UTF_8))