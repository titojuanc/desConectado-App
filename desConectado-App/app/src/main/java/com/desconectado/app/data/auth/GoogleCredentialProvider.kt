package com.desconectado.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.desconectado.app.domain.model.ErrorApp
import com.desconectado.app.domain.model.Resultado
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

/**
 * Obtiene el token de identidad de Google con el gestor de credenciales de Android. Necesita un
 * `Context` de actividad para mostrar el selector de cuentas; por eso vive fuera del repositorio,
 * que solo recibe el token ya obtenido.
 *
 * `webClientId` es el `default_web_client_id` que genera el plugin `google-services`.
 */
class GoogleCredentialProvider(private val webClientId: String) {

    suspend fun obtenerIdToken(actividad: Context): Resultado<String> {
        val opcion = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            // Muestra todas las cuentas de Google del dispositivo, no solo las ya autorizadas.
            .setFilterByAuthorizedAccounts(false)
            .build()
        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        return try {
            val credencial = CredentialManager.create(actividad).getCredential(actividad, pedido).credential
            if (credencial is CustomCredential &&
                credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Resultado.Exito(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
            } else {
                Resultado.Fallo(ErrorApp.Desconocido)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Resultado.Fallo(ErrorApp.Cancelado)
        } catch (e: NoCredentialException) {
            // No hay ninguna cuenta de Google en el dispositivo.
            Resultado.Fallo(ErrorApp.Desconocido)
        } catch (e: GetCredentialInterruptedException) {
            // Interrupción recuperable, típicamente por falta de red.
            Resultado.Fallo(ErrorApp.SinConexion)
        } catch (e: GetCredentialException) {
            Resultado.Fallo(ErrorApp.Desconocido)
        } catch (e: GoogleIdTokenParsingException) {
            Resultado.Fallo(ErrorApp.Desconocido)
        }
    }
}
