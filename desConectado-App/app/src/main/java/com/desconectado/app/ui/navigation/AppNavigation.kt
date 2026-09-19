package com.desconectado.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.desconectado.app.AppContainer
import com.desconectado.app.domain.model.Conectividad
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.ui.auth.AccionesVinculacion
import com.desconectado.app.ui.auth.EsperaScreen
import com.desconectado.app.ui.auth.IngresoScreen
import com.desconectado.app.ui.auth.IngresoViewModel
import com.desconectado.app.ui.auth.RegistroScreen
import com.desconectado.app.ui.auth.RegistroViewModel
import com.desconectado.app.ui.auth.RestablecerPasswordScreen
import com.desconectado.app.ui.auth.RestablecerPasswordViewModel
import com.desconectado.app.ui.auth.SesionViewModel
import com.desconectado.app.ui.challenges.DesafiosScreen
import com.desconectado.app.ui.challenges.DesafiosViewModel
import com.desconectado.app.ui.profile.PerfilScreen
import com.desconectado.app.ui.profile.PerfilViewModel
import com.desconectado.app.ui.rewards.RecompensasScreen
import com.desconectado.app.ui.rewards.RecompensasViewModel

private object Rutas {
    const val INGRESO = "ingreso"
    const val REGISTRO = "registro"
    const val RESTABLECER = "restablecer"
}

/** Raíz de la app: conecta el estado de la sesión con las pantallas que corresponden (FR-012). */
@Composable
fun AppNavigation(container: AppContainer) {
    val sesionViewModel: SesionViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SesionViewModel(container.authRepository, container.connectivityMonitor) }
        },
    )
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()
    val conectividad by container.connectivityMonitor.estado
        .collectAsStateWithLifecycle(initialValue = Conectividad.CONECTADO)

    RaizApp(
        estado = estado,
        conectividad = conectividad,
        sinSesion = { GrafoAcceso(container) },
        conSesion = {
            MainShell(
                conectividad = conectividad,
                desafios = { DesafiosRoute(container) },
                recompensas = { RecompensasRoute(container) },
                perfil = { PerfilRoute(container) },
            )
        },
    )
}

@Composable
private fun DesafiosRoute(container: AppContainer) {
    val viewModel: DesafiosViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DesafiosViewModel(container.catalogRepository, container.connectivityMonitor) }
        },
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    DesafiosScreen(estado = estado, onReintentar = viewModel::reintentar)
}

@Composable
private fun RecompensasRoute(container: AppContainer) {
    val viewModel: RecompensasViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecompensasViewModel(container.catalogRepository, container.connectivityMonitor) }
        },
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RecompensasScreen(estado = estado, onReintentar = viewModel::reintentar)
}

@Composable
private fun PerfilRoute(container: AppContainer) {
    val viewModel: PerfilViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PerfilViewModel(container.authRepository, container.perfilRepository, container.connectivityMonitor)
            }
        },
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PerfilScreen(estado = estado, onReintentar = viewModel::reintentar, onCerrarSesion = viewModel::cerrarSesion)
}

/**
 * Elige qué se muestra según la sesión: espera, acceso (solo sin sesión) o la navegación principal
 * (solo con sesión, también sin conexión y con el aviso visible; la sesión no se cierra por eso).
 */
@Composable
fun RaizApp(
    estado: EstadoSesion,
    conectividad: Conectividad,
    sinSesion: @Composable () -> Unit,
    conSesion: @Composable () -> Unit = { MainShell(conectividad = conectividad) },
) {
    when (estado) {
        EstadoSesion.Cargando -> EsperaScreen()
        EstadoSesion.SinSesion -> sinSesion()
        is EstadoSesion.ConSesion -> conSesion()
    }
}

/** Ingreso y Registro: lo único alcanzable sin sesión. */
@Composable
private fun GrafoAcceso(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Rutas.INGRESO) {
        composable(Rutas.INGRESO) {
            val viewModel: IngresoViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { IngresoViewModel(container.authRepository, container.connectivityMonitor) }
                },
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val contexto = LocalContext.current
            val alcance = rememberCoroutineScope()
            IngresoScreen(
                estado = estado,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onIngresar = viewModel::ingresar,
                onIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                onOlvidePassword = { navController.navigate(Rutas.RESTABLECER) },
                onGoogle = {
                    alcance.launch {
                        viewModel.continuarConGoogle(container.googleCredentialProvider.obtenerIdToken(contexto))
                    }
                },
                vinculacion = AccionesVinculacion(
                    onEmailChange = viewModel::onVinculacionEmailChange,
                    onPasswordChange = viewModel::onVinculacionPasswordChange,
                    onConfirmar = viewModel::confirmarVinculacion,
                    onCancelar = viewModel::cancelarVinculacion,
                ),
            )
        }
        composable(Rutas.RESTABLECER) {
            val viewModel: RestablecerPasswordViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        RestablecerPasswordViewModel(container.authRepository, container.connectivityMonitor)
                    }
                },
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            RestablecerPasswordScreen(
                estado = estado,
                onEmailChange = viewModel::onEmailChange,
                onEnviar = viewModel::enviar,
                onVolver = { navController.popBackStack() },
            )
        }
        composable(Rutas.REGISTRO) {
            val viewModel: RegistroViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { RegistroViewModel(container.authRepository, container.connectivityMonitor) }
                },
            )
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val contexto = LocalContext.current
            val alcance = rememberCoroutineScope()
            RegistroScreen(
                estado = estado,
                onUsernameChange = viewModel::onUsernameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRegistrar = viewModel::registrar,
                onIrAIngreso = { navController.popBackStack() },
                onGoogle = {
                    alcance.launch {
                        viewModel.continuarConGoogle(container.googleCredentialProvider.obtenerIdToken(contexto))
                    }
                },
                vinculacion = AccionesVinculacion(
                    onEmailChange = viewModel::onVinculacionEmailChange,
                    onPasswordChange = viewModel::onVinculacionPasswordChange,
                    onConfirmar = viewModel::confirmarVinculacion,
                    onCancelar = viewModel::cancelarVinculacion,
                ),
            )
        }
    }
}
