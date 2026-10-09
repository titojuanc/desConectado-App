package com.desconectado.app.data.auth

import com.desconectado.app.data.esErrorDeRed
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.EstadoSesion
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.model.UserPreferences
import com.desconectado.app.domain.normalizarCorreo
import com.desconectado.app.domain.repository.AuthRepository
import com.desconectado.app.domain.repository.ProfileRepository
import com.desconectado.app.domain.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Cuentas y sesión sobre Firebase Authentication (contracts/repositories.md). */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val perfiles: ProfileRepository,
    private val userPreferences: UserPreferencesRepository,
) : AuthRepository {

    override val authState: Flow<EstadoSesion> = callbackFlow {
        trySend(EstadoSesion.Cargando)
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val usuario = firebaseAuth.currentUser
            trySend(if (usuario == null) EstadoSesion.SinSesion else EstadoSesion.ConSesion(usuario.uid))
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun registrar(
        username: String,
        email: String,
        password: String,
        weeklyGoalMinutes: Int,
    ): Resultado<Unit> {
        if (!weeklyGoalMinutes.esMetaValida()) return Resultado.Fallo(ErrorApp.Desconocido)
        val correo = normalizarCorreo(email)
        return try {
            val usuario = auth.createUserWithEmailAndPassword(correo, password).await().user
                ?: return Resultado.Fallo(ErrorApp.Desconocido)
            // Si el perfil falla, la cuenta queda creada; `ingresar` lo completa en el próximo intento.
            when (val perfil = perfiles.asegurarPerfil(usuario.uid, username.trim(), correo)) {
                is Resultado.Fallo -> perfil
                is Resultado.Exito -> guardarMetaSemanal(usuario.uid, weeklyGoalMinutes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.Fallo(
                when {
                    e is FirebaseAuthUserCollisionException -> ErrorApp.CorreoEnUso
                    e.esErrorDeRed() -> ErrorApp.SinConexion
                    else -> ErrorApp.Desconocido
                },
            )
        }
    }

    override suspend fun ingresar(email: String, password: String): Resultado<Unit> {
        val correo = normalizarCorreo(email)
        return try {
            val usuario = auth.signInWithEmailAndPassword(correo, password).await().user
                ?: return Resultado.Fallo(ErrorApp.Desconocido)
            // Por si un registro anterior quedó a medias y la cuenta no tiene perfil. Es un intento
            // de mejor esfuerzo: la sesión ya está iniciada y la pantalla de Perfil reintenta.
            perfiles.asegurarPerfil(usuario.uid, usuario.displayName, correo)
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.Fallo(
                when {
                    e is FirebaseAuthInvalidCredentialsException || e is FirebaseAuthInvalidUserException ->
                        ErrorApp.CredencialesInvalidas
                    e.esErrorDeRed() -> ErrorApp.SinConexion
                    else -> ErrorApp.Desconocido
                },
            )
        }
    }

    override suspend fun ingresarConGoogle(idToken: String, weeklyGoalMinutes: Int?): Resultado<Unit> {
        if (weeklyGoalMinutes != null && !weeklyGoalMinutes.esMetaValida()) {
            return Resultado.Fallo(ErrorApp.Desconocido)
        }
        return try {
            val credencial = GoogleAuthProvider.getCredential(idToken, null)
            val usuario = auth.signInWithCredential(credencial).await().user
                ?: return Resultado.Fallo(ErrorApp.Desconocido)
            val correo = usuario.email ?: return Resultado.Fallo(ErrorApp.Desconocido)
            // Cuenta nueva: crea el perfil con el nombre de Google (o la parte local del correo).
            // Cuenta que ya existía: `asegurarPerfil` no toca el perfil que ya tiene.
            when (val perfil = perfiles.asegurarPerfil(usuario.uid, usuario.displayName, correo)) {
                is Resultado.Fallo -> perfil
                is Resultado.Exito -> if (weeklyGoalMinutes == null) {
                    Resultado.Exito(Unit)
                } else {
                    when (val current = userPreferences.leer(usuario.uid)) {
                        is Resultado.Exito -> if (current.valor.weeklyGoalMinutes == null) {
                            guardarMetaSemanal(usuario.uid, weeklyGoalMinutes)
                        } else {
                            Resultado.Exito(Unit)
                        }
                        is Resultado.Fallo -> current
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthUserCollisionException) {
            // El correo ya tiene una cuenta con otro proveedor y Firebase no las unificó solo: la
            // interfaz pide la contraseña de esa cuenta y llama a `vincularGoogle`.
            Resultado.Fallo(ErrorApp.CuentaExistenteConOtroProveedor)
        } catch (e: Exception) {
            Resultado.Fallo(if (e.esErrorDeRed()) ErrorApp.SinConexion else ErrorApp.Desconocido)
        }
    }

    override suspend fun vincularGoogle(idToken: String): Resultado<Unit> {
        val usuario = auth.currentUser ?: return Resultado.Fallo(ErrorApp.Desconocido)
        return try {
            usuario.linkWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.Fallo(if (e.esErrorDeRed()) ErrorApp.SinConexion else ErrorApp.Desconocido)
        }
    }

    override suspend fun restablecerPassword(email: String): Resultado<Unit> {
        val correo = normalizarCorreo(email)
        // El correo llega en español, con la plantilla configurada en la consola de Firebase.
        auth.setLanguageCode("es")
        return try {
            auth.sendPasswordResetEmail(correo).await()
            Resultado.Exito(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // Cuenta inexistente: se responde igual que si existiera para no revelar qué correos
            // están registrados (FR-026).
            Resultado.Exito(Unit)
        } catch (e: Exception) {
            Resultado.Fallo(if (e.esErrorDeRed()) ErrorApp.SinConexion else ErrorApp.Desconocido)
        }
    }

    override fun cerrarSesion() {
        auth.signOut()
    }

    override suspend fun verificarCuenta() {
        val usuario = auth.currentUser ?: return
        try {
            usuario.reload().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // La cuenta ya no existe o fue deshabilitada: se cierra la sesión.
            auth.signOut()
        } catch (e: Exception) {
            // Sin red u otro fallo transitorio: la sesión NO se cierra (FR-011).
        }
    }

    private fun Int.esMetaValida() = this in UserPreferences.META_MINIMA..UserPreferences.META_MAXIMA &&
        this % UserPreferences.INCREMENTO_META == 0

    private suspend fun guardarMetaSemanal(uid: String, minutes: Int): Resultado<Unit> =
        when (val result = userPreferences.guardarMetaSemanal(uid, minutes)) {
            is Resultado.Exito -> Resultado.Exito(Unit)
            is Resultado.Fallo -> result
        }
}
