package com.desconectado.app.domain.repository

import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import kotlinx.coroutines.flow.Flow

interface CosmeticPreferencesRepository {
    suspend fun leer(uid: String): Resultado<CosmeticPreferences>
    suspend fun seleccionar(uid: String, kind: TipoRecompensa, rewardId: String?): Resultado<CosmeticPreferences>
    fun observar(uid: String): Flow<CosmeticPreferences>
}