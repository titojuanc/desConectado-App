package com.desconectado.app.fakes

import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de `AuthRepository`: resultados y estado de sesión programables, registro de llamadas y
 * una compuerta para simular una petición lenta (mientras esté cerrada, las operaciones de red
 * quedan esperando).
 */
class FakeAuthRepository(estadoInicial: EstadoSesion = EstadoSesion.SinSesion) : AuthRepository {

    private val estado = MutableStateFlow(estadoInicial)
    override val authState: Flow<EstadoSesion> = estado

    var resultadoRegistrar: Resultado<Unit> = Resultado.Exito(Unit)
    var resultadoIngresar: Resultado<Unit> = Resultado.Exito(Unit)
    var resultadoIngresarConGoogle: Resultado<Unit> = Resultado.Exito(Unit)
    var resultadoVincularGoogle: Resultado<Unit> = Resultado.Exito(Unit)
    var resultadoRestablecer: Resultado<Unit> = Resultado.Exito(Unit)

    /** Si no es nulo, cada operación de red espera a que se complete antes de responder. */
    var compuerta: CompletableDeferred<Unit>? = null

    val llamadasRegistrar = mutableListOf<Triple<String, String, String>>()
    val llamadasIngresar = mutableListOf<Pair<String, String>>()
    val llamadasIngresarConGoogle = mutableListOf<String>()
    val llamadasVincularGoogle = mutableListOf<String>()
    val llamadasRestablecer = mutableListOf<String>()
    var cierresDeSesion = 0
        private set
    var verificaciones = 0
        private set

    fun establecerEstado(nuevo: EstadoSesion) {
        estado.value = nuevo
    }

    override suspend fun registrar(username: String, email: String, password: String): Resultado<Unit> {
        llamadasRegistrar += Triple(username, email, password)
        compuerta?.await()
        return resultadoRegistrar
    }

    override suspend fun ingresar(email: String, password: String): Resultado<Unit> {
        llamadasIngresar += email to password
        compuerta?.await()
        return resultadoIngresar
    }

    override suspend fun ingresarConGoogle(idToken: String): Resultado<Unit> {
        llamadasIngresarConGoogle += idToken
        compuerta?.await()
        return resultadoIngresarConGoogle
    }

    override suspend fun vincularGoogle(idToken: String): Resultado<Unit> {
        llamadasVincularGoogle += idToken
        compuerta?.await()
        return resultadoVincularGoogle
    }

    override suspend fun restablecerPassword(email: String): Resultado<Unit> {
        llamadasRestablecer += email
        compuerta?.await()
        return resultadoRestablecer
    }

    override fun cerrarSesion() {
        cierresDeSesion++
        estado.value = EstadoSesion.SinSesion
    }

    override suspend fun verificarCuenta() {
        verificaciones++
    }
}
