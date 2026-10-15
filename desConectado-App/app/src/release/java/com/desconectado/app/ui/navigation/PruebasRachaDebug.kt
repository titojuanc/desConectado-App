package com.desconectado.app.ui.navigation

import com.desconectado.app.AppContainer

suspend fun ejecutarPruebaRacha(container: AppContainer, uid: String, accion: AccionRachaDebug): String =
    error("Las pruebas de racha no están disponibles en release.")
