package com.desconectado.app.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.UsageStatsRepository
import java.time.Instant

class AndroidUsageStatsRepository(
    private val context: Context,
) : UsageStatsRepository {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        if (mode == AppOpsManager.MODE_ALLOWED) return true
        if (mode != AppOpsManager.MODE_DEFAULT) return false
        val now = System.currentTimeMillis()
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 24 * 60 * 60 * 1000L,
            now,
        ).isNotEmpty()
    }

    override fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override suspend fun socialUsageSeconds(start: Instant, end: Instant): Resultado<Long> {
        return when (val detalle = socialUsageByPackageSeconds(start, end)) {
            is Resultado.Exito -> Resultado.Exito(detalle.valor.values.sum())
            is Resultado.Fallo -> detalle
        }
    }

    override suspend fun socialUsageByPackageSeconds(start: Instant, end: Instant): Resultado<Map<String, Long>> {
        if (!hasUsageAccess()) return Resultado.Fallo(ErrorApp.AccesoUsoDenegado)
        if (end <= start) return Resultado.Exito(emptyMap())
        val events = try {
            usageStatsManager.queryEvents(start.toEpochMilli(), end.toEpochMilli())
        } catch (_: SecurityException) {
            return Resultado.Fallo(ErrorApp.AccesoUsoDenegado)
        }
        val event = UsageEvents.Event()
        val openAt = mutableMapOf<String, Long>()
        val total = mutableMapOf<String, Long>().withDefault { 0L }
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName !in TARGET_PACKAGES) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> openAt[packageName] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val foregroundAt = openAt.remove(packageName) ?: continue
                    total[packageName] = total.getValue(packageName) + (event.timeStamp - foregroundAt).coerceAtLeast(0L)
                }
            }
        }
        val endMillis = end.toEpochMilli()
        openAt.forEach { (packageName, foregroundAt) ->
            total[packageName] = total.getValue(packageName) + (endMillis - foregroundAt).coerceAtLeast(0L)
        }
        return Resultado.Exito(total.mapValues { it.value / 1000L })
    }

    companion object {
        val TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android",
            "com.google.android.youtube",
        )
    }
}
