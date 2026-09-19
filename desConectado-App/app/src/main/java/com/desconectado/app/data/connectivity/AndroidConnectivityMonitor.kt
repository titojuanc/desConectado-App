package com.desconectado.app.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Publica la conectividad del dispositivo a partir de la red activa del sistema. */
class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {

    private val manager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val estado: Flow<Conectividad> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(estadoActual())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(aConectividad(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(estadoActual())
            }
        }
        trySend(estadoActual())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }

    private fun estadoActual(): Conectividad {
        val capacidades = manager.getNetworkCapabilities(manager.activeNetwork)
        return if (capacidades == null) Conectividad.SIN_CONEXION else aConectividad(capacidades)
    }

    private fun aConectividad(capacidades: NetworkCapabilities): Conectividad {
        val conInternet = capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (conInternet) Conectividad.CONECTADO else Conectividad.SIN_CONEXION
    }
}
