package com.desconectado.app.domain

import com.desconectado.app.domain.model.PointLot
import java.time.Instant

fun consumirPuntosFifo(lots: List<PointLot>, points: Int, moment: Instant): List<PointLot> {
    require(points >= 0)
    require(lots.map { it.lotId }.toSet().size == lots.size)
    val available = lots.filter { moment.isBefore(it.expiresAt) }.sumOf { it.remainingPoints.toLong() }
    require(available >= points)

    var pending = points
    return lots.sortedWith(compareBy<PointLot> { it.earnedAt }.thenBy { it.lotId }).map { lot ->
        if (pending == 0 || !moment.isBefore(lot.expiresAt)) return@map lot
        val consumed = minOf(lot.remainingPoints, pending)
        pending -= consumed
        lot.copy(remainingPoints = lot.remainingPoints - consumed)
    }
}