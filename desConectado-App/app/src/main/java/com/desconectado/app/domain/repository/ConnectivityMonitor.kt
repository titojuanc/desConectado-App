package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.Conectividad
import kotlinx.coroutines.flow.Flow

/** Publica si el dispositivo tiene conexión a Internet. */
interface ConnectivityMonitor {
    val estado: Flow<Conectividad>
}
