package com.desconectado.app.fakes

import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.repository.ConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Conectividad controlable desde la prueba. */
class FakeConnectivityMonitor(inicial: Conectividad = Conectividad.CONECTADO) : ConnectivityMonitor {

    private val flujo = MutableStateFlow(inicial)

    override val estado: Flow<Conectividad> = flujo

    fun establecer(conectividad: Conectividad) {
        flujo.value = conectividad
    }
}
