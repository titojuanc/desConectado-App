package com.desconectado.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.desconectado.app.ui.navigation.AppNavigation
import com.desconectado.app.ui.theme.DesConectadoTheme
import com.desconectado.app.domain.model.CosmeticPreferences
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.TipoRecompensa
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4201)
        }
        // El tema de la app es oscuro siempre; los íconos del sistema deben ser claros.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as DesConectadoApp).container
        setContent {
            val session by container.authRepository.authState.collectAsState(initial = EstadoSesion.Cargando)
            val uid = (session as? EstadoSesion.ConSesion)?.uid
            val preferencesFlow = remember(uid) {
                uid?.let(container.cosmeticPreferencesRepository::observar) ?: flowOf(CosmeticPreferences())
            }
            val preferences by preferencesFlow.collectAsState(initial = CosmeticPreferences())
            DesConectadoTheme(temaId = preferences.activeCosmetics[TipoRecompensa.TEMA]) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(container, preferences)
                }
            }
        }
    }
}
