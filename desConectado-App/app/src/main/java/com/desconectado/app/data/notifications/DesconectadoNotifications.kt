package com.desconectado.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.desconectado.app.R

class DesconectadoNotifications(private val context: Context) {
    init {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Desafíos", NotificationManager.IMPORTANCE_DEFAULT),
                )
            }
        } catch (e: RuntimeException) {
            Log.e(CHANNEL_ID, "No se pudo crear el canal de notificaciones", e)
        }
    }

    fun desafioTerminado(cumplido: Boolean, puntos: Int, enabled: Boolean) {
        val titulo = if (cumplido) "Desafío cumplido" else "Desafío terminado"
        val texto = if (cumplido) "Ganaste $puntos puntos." else "El desafío no se cumplió."
        mostrar(titulo, texto, 1001, enabled)
    }

    fun entroAUnaRed(nombre: String, enabled: Boolean) {
        mostrar("Desafío perdido", "Usaste $nombre durante el desafío.", 1002, enabled)
    }

    fun logrosDesbloqueados(nombres: List<String>, enabled: Boolean) {
        if (nombres.isEmpty()) return
        mostrar("Logro desbloqueado", nombres.joinToString(", "), 1003, enabled)
    }

    private fun mostrar(titulo: String, texto: String, id: Int, enabled: Boolean) {
        if (!enabled) return
        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
            NotificationManagerCompat.from(context).notify(
                id,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(titulo)
                    .setContentText(texto)
                    .setAutoCancel(true)
                    .build(),
            )
        } catch (e: RuntimeException) {
            Log.e(CHANNEL_ID, "No se pudo mostrar la notificación $id; el resultado se conserva en la app", e)
        }
    }

    private companion object { const val CHANNEL_ID = "desafios" }
}
