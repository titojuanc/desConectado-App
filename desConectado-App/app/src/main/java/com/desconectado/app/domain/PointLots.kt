package com.desconectado.app.domain

import com.desconectado.app.domain.model.PointLot
import com.desconectado.app.domain.model.UpcomingPointExpiry
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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

fun proximoVencimiento(lots: List<PointLot>, now: Instant): UpcomingPointExpiry? {
    val nextExpiration = lots.asSequence()
        .filter { it.remainingPoints > 0 && it.expiresAt.isAfter(now) }
        .minOfOrNull { it.expiresAt }
        ?: return null
    val matchingLots = lots.filter { it.remainingPoints > 0 && it.expiresAt == nextExpiration }
    val zone = ZoneId.of(matchingLots.first().timeZoneId)
    val today = now.atZone(zone).toLocalDate()
    val expiryDate = nextExpiration.atZone(zone).toLocalDate()
    val daysRemaining = ChronoUnit.DAYS.between(today, expiryDate).coerceAtLeast(0)
    val points = matchingLots.sumOf { it.remainingPoints.toLong() }

    return UpcomingPointExpiry(
        points = points.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        expiresAt = nextExpiration,
        daysRemaining = daysRemaining,
    )
}