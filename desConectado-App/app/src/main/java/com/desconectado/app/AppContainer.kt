package com.desconectado.app

import android.content.Context
import com.desconectado.app.data.auth.FirebaseAuthRepository
import com.desconectado.app.data.auth.GoogleCredentialProvider
import com.desconectado.app.data.catalog.FirestoreCatalogRepository
import com.desconectado.app.data.connectivity.AndroidConnectivityMonitor
import com.desconectado.app.data.points.FirestorePointsRepository
import com.desconectado.app.data.profile.FirestoreProfileRepository
import com.desconectado.app.data.profile.FirestoreAchievementRepository
import com.desconectado.app.data.profile.FirestoreCosmeticPreferencesRepository
import com.desconectado.app.data.profile.FirestoreUserPreferencesRepository
import com.desconectado.app.data.usage.AndroidUsageStatsRepository
import com.desconectado.app.data.notifications.DesconectadoNotifications
import com.desconectado.app.data.challenges.ActiveChallengeStore
import com.desconectado.app.data.challenges.FirestoreChallengeRepository
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.CatalogRepository
import com.desconectado.app.domain.repository.ConnectivityMonitor
import com.desconectado.app.domain.repository.PointsRepository
import com.desconectado.app.domain.repository.ProfileRepository
import com.desconectado.app.domain.repository.AchievementRepository
import com.desconectado.app.domain.repository.CosmeticPreferencesRepository
import com.desconectado.app.domain.repository.UserPreferencesRepository
import com.desconectado.app.domain.repository.UsageStatsRepository
import com.desconectado.app.domain.repository.ChallengeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

/**
 * Contenedor de dependencias de la app. Los repositorios se agregan en cada historia.
 *
 * Con `USE_FIREBASE_EMULATOR` los emuladores se configuran antes de cualquier uso de Auth o
 * Firestore. Firestore usa solo caché en memoria: nada del catálogo ni del perfil queda en disco.
 */
class AppContainer(context: Context) {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            auth.useEmulator(BuildConfig.EMULATOR_HOST, PUERTO_EMULADOR_AUTH)
            firestore.useEmulator(BuildConfig.EMULATOR_HOST, PUERTO_EMULADOR_FIRESTORE)
        }
        // Parte de los ajustes vigentes para conservar el host del emulador si se configuró.
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }

    val connectivityMonitor: ConnectivityMonitor = AndroidConnectivityMonitor(context)

    val perfilRepository: ProfileRepository = FirestoreProfileRepository(firestore)
    val achievementRepository: AchievementRepository = FirestoreAchievementRepository(firestore)
    val cosmeticPreferencesRepository: CosmeticPreferencesRepository = FirestoreCosmeticPreferencesRepository(firestore)
    val userPreferencesRepository: UserPreferencesRepository = FirestoreUserPreferencesRepository(firestore)
    val authRepository: AuthRepository = FirebaseAuthRepository(auth, perfilRepository, userPreferencesRepository)
    val catalogRepository: CatalogRepository = FirestoreCatalogRepository(firestore)
    val pointsRepository: PointsRepository = FirestorePointsRepository(firestore)
    val usageStatsRepository: UsageStatsRepository = AndroidUsageStatsRepository(context)
    val activeChallengeStore = ActiveChallengeStore(context)
    val challengeRepository: ChallengeRepository = FirestoreChallengeRepository(firestore, activeChallengeStore)
    val notifications = DesconectadoNotifications(context)

    val googleCredentialProvider = GoogleCredentialProvider(context.getString(R.string.default_web_client_id))

    private companion object {
        const val PUERTO_EMULADOR_AUTH = 9099
        const val PUERTO_EMULADOR_FIRESTORE = 8080
    }
}
