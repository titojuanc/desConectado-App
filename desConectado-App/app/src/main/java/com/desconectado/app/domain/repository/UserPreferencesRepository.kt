package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    suspend fun leer(uid: String): Resultado<UserPreferences>
    fun observar(uid: String): Flow<UserPreferences>
    suspend fun guardarMetaSemanal(uid: String, minutes: Int): Resultado<UserPreferences>
    suspend fun configurarNotificaciones(uid: String, enabled: Boolean): Resultado<UserPreferences>
}