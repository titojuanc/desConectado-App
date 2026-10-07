package com.desconectado.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.util.UUID
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
import com.desconectado.app.ui.challenges.DesafioActivoScreen
import com.desconectado.app.ui.challenges.DesafioActivoViewModel
import com.desconectado.app.ui.profile.PerfilScreen
import com.desconectado.app.ui.points.SaldoViewModel
import com.desconectado.app.ui.profile.PerfilViewModel
import com.desconectado.app.ui.rewards.RecompensasScreen
import com.desconectado.app.ui.rewards.RecompensasViewModel
import com.desconectado.app.ui.rewards.RecompensasCanjeUiState

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
        conSesion = { ShellConSesion(container, conectividad) },
    )
}

/** Navegación principal con el saldo de puntos compartido por las tres pestañas (FR-028). */
@Composable
private fun ShellConSesion(container: AppContainer, conectividad: Conectividad) {
    val saldoViewModel: SaldoViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SaldoViewModel(container.authRepository, container.perfilRepository, container.connectivityMonitor)
            }
        },
    )
    val saldo by saldoViewModel.estado.collectAsStateWithLifecycle()
    MainShell(
        conectividad = conectividad,
        saldo = saldo,
        onDestinoCambiado = saldoViewModel::recargar,
        desafios = { DesafiosRoute(container) },
        recompensas = { RecompensasRoute(container) },
        perfil = { PerfilRoute(container) },
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
    var desafioSeleccionado by remember { mutableStateOf<com.desconectado.app.domain.model.Desafio?>(null) }
    val seleccionado = desafioSeleccionado
    if (seleccionado == null) {
        DesafiosScreen(
            estado = estado,
            onReintentar = viewModel::reintentar,
            onIniciar = { desafioSeleccionado = it },
        )
    } else {
        val activoViewModel: DesafioActivoViewModel = viewModel(
            key = "desafio-activo-${seleccionado.id}",
            factory = viewModelFactory {
                initializer {
                    DesafioActivoViewModel(
                        uid = container.auth.currentUser?.uid.orEmpty(),
                        challenges = container.challengeRepository,
                        usage = container.usageStatsRepository,
                        store = container.activeChallengeStore,
                        points = container.pointsRepository,
                        notifications = container.notifications,
                        connectivity = container.connectivityMonitor,
                    )
                }
            },
        )
        val activoEstado by activoViewModel.estado.collectAsStateWithLifecycle()
        DesafioActivoScreen(
            estado = activoEstado,
            onAbrirAjustes = activoViewModel::abrirAjustes,
            onReintentarPermiso = activoViewModel::reintentarPermiso,
            onActualizar = activoViewModel::actualizar,
            onFinalizar = activoViewModel::finalizar,
            onCancelar = activoViewModel::cancelar,
            onVolverCatalogo = { desafioSeleccionado = null },
        )
        androidx.compose.runtime.LaunchedEffect(seleccionado) {
            activoViewModel.iniciar(seleccionado)
        }
    }
}

@Composable
private fun RecompensasRoute(container: AppContainer) {
    val viewModel: RecompensasViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecompensasViewModel(container.catalogRepository, container.connectivityMonitor) }
        },
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val canjeViewModel: com.desconectado.app.ui.rewards.RecompensasCanjeViewModel = viewModel(
        key = "recompensas-canje",
        factory = viewModelFactory {
            initializer {
                com.desconectado.app.ui.rewards.RecompensasCanjeViewModel(
                    uid = container.auth.currentUser?.uid.orEmpty(),
                    catalog = container.catalogRepository,
                    points = container.pointsRepository,
                    connectivity = container.connectivityMonitor,
                )
            }
        },
    )
    val canjeEstado by canjeViewModel.estado.collectAsStateWithLifecycle()
    val canjePendiente = (canjeEstado as? RecompensasCanjeUiState.Lista)?.pendiente
    RecompensasScreen(
        estado = estado,
        onReintentar = viewModel::reintentar,
        onCanjear = { canjeViewModel.canjear(it, UUID.randomUUID().toString()) },
        feedbackEvents = canjeViewModel.feedback,
        canjePendiente = canjePendiente,
    )
}

@Composable
private fun PerfilRoute(container: AppContainer) {
    val viewModel: PerfilViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PerfilViewModel(
                    container.authRepository,
                    container.perfilRepository,
                    container.challengeRepository,
                    container.pointsRepository,
                    container.connectivityMonitor,
                )
            }
        },
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val desafiosHechos by viewModel.desafiosHechos.collectAsStateWithLifecycle()
    val canjes by viewModel.canjes.collectAsStateWithLifecycle()
    PerfilScreen(
        estado = estado,
        desafiosHechos = desafiosHechos,
        canjes = canjes,
        onReintentar = viewModel::reintentar,
        onReintentarPuntos = viewModel::reintentarHistorial,
        onReintentarCanjes = viewModel::reintentarCanjes,
        onCerrarSesion = viewModel::cerrarSesion,
    )
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
