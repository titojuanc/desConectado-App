package com.desconectado.app.data.challenges

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.desconectado.app.domain.model.ActiveChallenge
import kotlinx.coroutines.flow.first
import java.time.Instant

private val Context.activeChallengeDataStore by preferencesDataStore(name = "active_challenge")

class ActiveChallengeStore(private val context: Context) {
    suspend fun read(): ActiveChallenge? {
        val values = context.activeChallengeDataStore.data.first()
        val id = values[CHALLENGE_ID] ?: return null
        return ActiveChallenge(
            challengeId = id,
            challengeTitle = values[CHALLENGE_TITLE] ?: return null,
            durationMinutes = values[DURATION_MINUTES] ?: return null,
            points = values[POINTS] ?: return null,
            startedAt = Instant.ofEpochMilli(values[STARTED_AT] ?: return null),
            offlineSeconds = values[OFFLINE_SECONDS] ?: 0L,
            status = values[STATUS]?.let { runCatching { ActiveChallenge.Status.valueOf(it) }.getOrNull() } ?: return null,
            updatedAt = Instant.ofEpochMilli(values[UPDATED_AT] ?: return null),
            durationSeconds = values[DURATION_SECONDS] ?: (values[DURATION_MINUTES] ?: 0) * 60,
            category = values[CATEGORY]?.let(com.desconectado.app.domain.model.CategoriaDesafio::desdeAlmacen),
        )
    }

    suspend fun write(challenge: ActiveChallenge) {
        context.activeChallengeDataStore.edit { values ->
            values[CHALLENGE_ID] = challenge.challengeId
            values[CHALLENGE_TITLE] = challenge.challengeTitle
            values[DURATION_MINUTES] = challenge.durationMinutes
            values[POINTS] = challenge.points
            values[STARTED_AT] = challenge.startedAt.toEpochMilli()
            values[OFFLINE_SECONDS] = challenge.offlineSeconds
            values[STATUS] = challenge.status.name
            values[UPDATED_AT] = challenge.updatedAt.toEpochMilli()
            values[DURATION_SECONDS] = challenge.durationSeconds
            challenge.category?.let { values[CATEGORY] = it.valorAlmacen } ?: values.remove(CATEGORY)
        }
    }

    suspend fun clear() {
        context.activeChallengeDataStore.edit { it.clear() }
    }

    private companion object {
        val CHALLENGE_ID = stringPreferencesKey("challenge_id")
        val CHALLENGE_TITLE = stringPreferencesKey("challenge_title")
        val DURATION_MINUTES = androidx.datastore.preferences.core.intPreferencesKey("duration_minutes")
        val POINTS = androidx.datastore.preferences.core.intPreferencesKey("points")
        val STARTED_AT = longPreferencesKey("started_at")
        val OFFLINE_SECONDS = longPreferencesKey("offline_seconds")
        val STATUS = stringPreferencesKey("status")
        val UPDATED_AT = longPreferencesKey("updated_at")
        val DURATION_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("duration_seconds")
        val CATEGORY = stringPreferencesKey("category")
    }
}
