package com.desconectado.app.fakes

import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.TipoRecompensa
import com.desconectado.app.domain.repository.CosmeticPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCosmeticPreferencesRepository : CosmeticPreferencesRepository {
    var resultado: Resultado<CosmeticPreferences> = Resultado.Exito(CosmeticPreferences())
    val selecciones = mutableListOf<Triple<String, TipoRecompensa, String?>>()
    private val flujo = MutableStateFlow(CosmeticPreferences())

    override suspend fun leer(uid: String) = resultado

    override fun observar(uid: String): Flow<CosmeticPreferences> = flujo

    override suspend fun seleccionar(
        uid: String,
        kind: TipoRecompensa,
        rewardId: String?,
    ): Resultado<CosmeticPreferences> {
        selecciones += Triple(uid, kind, rewardId)
        val current = (resultado as? Resultado.Exito)?.valor ?: return resultado
        val active = current.activeCosmetics.toMutableMap()
        if (rewardId == null) active.remove(kind) else active[kind] = rewardId
        val updated = CosmeticPreferences(active)
        flujo.value = updated
        return Resultado.Exito(updated).also { resultado = it }
    }
}