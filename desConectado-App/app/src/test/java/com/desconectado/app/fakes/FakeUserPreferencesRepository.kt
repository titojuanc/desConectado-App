package com.desconectado.app.fakes

import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    var preferences = UserPreferences()
    var readResult: Resultado<UserPreferences> = Resultado.Exito(preferences)
    private val flow = MutableStateFlow(preferences)
    val goalCalls = mutableListOf<Pair<String, Int>>()
    val notificationCalls = mutableListOf<Pair<String, Boolean>>()

    override suspend fun leer(uid: String): Resultado<UserPreferences> = readResult

    override fun observar(uid: String): Flow<UserPreferences> = flow

    override suspend fun guardarMetaSemanal(uid: String, minutes: Int): Resultado<UserPreferences> {
        goalCalls += uid to minutes
        preferences = preferences.copy(weeklyGoalMinutes = minutes)
        flow.value = preferences
        return Resultado.Exito(preferences)
    }

    override suspend fun configurarNotificaciones(uid: String, enabled: Boolean): Resultado<UserPreferences> {
        notificationCalls += uid to enabled
        preferences = preferences.copy(notificationsEnabled = enabled)
        flow.value = preferences
        return Resultado.Exito(preferences)
    }
}