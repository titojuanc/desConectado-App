package com.desconectado.app.data.usage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.usagePermissionTutorialDataStore by preferencesDataStore(name = "usage_permission_tutorial")

class UsagePermissionTutorialStore(private val context: Context) {
    suspend fun fueMostrado(): Boolean =
        context.usagePermissionTutorialDataStore.data.first()[MOSTRADO] ?: false

    suspend fun marcarMostrado() {
        context.usagePermissionTutorialDataStore.edit { preferences ->
            preferences[MOSTRADO] = true
        }
    }

    private companion object {
        val MOSTRADO = booleanPreferencesKey("shown")
    }
}