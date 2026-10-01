package com.desconectado.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.desconectado.app.R

class DesconectadoNotifications(private val context: Context) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Desafíos", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    fun desafioTerminado(cumplido: Boolean, puntos: Int) {
        val titulo = if (cumplido) "Desafío cumplido" else "Desafío terminado"
        val texto = if (cumplido) "Ganaste $puntos puntos." else "El desafío no se cumplió."
        mostrar(titulo, texto, 1001)
    }

    fun entroAUnaRed(nombre: String) {
        mostrar("Desafío perdido", "Usaste $nombre durante el desafío.", 1002)
    }

    private fun mostrar(titulo: String, texto: String, id: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            !context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS).equals(android.content.pm.PackageManager.PERMISSION_GRANTED)
        ) return
        NotificationManagerCompat.from(context).notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object { const val CHANNEL_ID = "desafios" }
}
