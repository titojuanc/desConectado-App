package com.desconectado.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

data class PointLot(
    val lotId: String,
    val localDate: LocalDate,
    val timeZoneId: String,
    val windowEndsAt: Instant,
    val expiresAt: Instant,
    val earnedAt: Instant,
    val issuedPoints: Int,
    val remainingPoints: Int,
) {
    val windowStartsAt: Instant
        get() = localDate.atStartOfDay(ZoneId.of(timeZoneId)).toInstant()

    init {
        require(issuedPoints > 0)
        require(remainingPoints in 0..issuedPoints)
        require(windowEndsAt.isAfter(earnedAt))
        require(expiresAt.isAfter(windowEndsAt))
    }

    fun agregar(points: Int, finishedAt: Instant): PointLot {
        require(points > 0)
        require(!finishedAt.isBefore(windowStartsAt) && finishedAt.isBefore(windowEndsAt))
        return copy(
            earnedAt = minOf(earnedAt, finishedAt),
            issuedPoints = Math.addExact(issuedPoints, points),
            remainingPoints = Math.addExact(remainingPoints, points),
        )
    }

    fun puntosVencidos(serverNow: Instant): Int =
        if (!serverNow.isBefore(expiresAt)) remainingPoints else 0

    companion object {
        fun crear(points: Int, finishedAt: Instant, zone: ZoneId): PointLot {
            require(points > 0)
            val localDate = finishedAt.atZone(zone).toLocalDate()
            val windowEndsAt = localDate.plusDays(1).atStartOfDay(zone).toInstant()
            val expiresAt = localDate.plusDays(30).atStartOfDay(zone).toInstant()
            val encodedZone = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(zone.id.toByteArray(Charsets.UTF_8))
            return PointLot(
                lotId = "daily-$localDate-$encodedZone",
                localDate = localDate,
                timeZoneId = zone.id,
                windowEndsAt = windowEndsAt,
                expiresAt = expiresAt,
                earnedAt = finishedAt,
                issuedPoints = points,
                remainingPoints = points,
            )
        }
    }
}

data class UpcomingPointExpiry(
    val points: Int,
    val expiresAt: Instant,
    val daysRemaining: Long,
)