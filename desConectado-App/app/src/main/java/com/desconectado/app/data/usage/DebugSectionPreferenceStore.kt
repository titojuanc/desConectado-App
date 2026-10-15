package com.desconectado.app.data.usage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.debugSectionDataStore by preferencesDataStore(name = "debug_section")

class DebugSectionPreferenceStore(private val context: Context) {
    suspend fun estaHabilitada(): Boolean =
        context.debugSectionDataStore.data.first()[HABILITADA] ?: false

    suspend fun guardar(habilitada: Boolean) {
        context.debugSectionDataStore.edit { preferences ->
            preferences[HABILITADA] = habilitada
        }
    }

    private companion object {
        val HABILITADA = booleanPreferencesKey("enabled")
    }
}