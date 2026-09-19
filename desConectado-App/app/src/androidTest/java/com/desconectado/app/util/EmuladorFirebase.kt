package com.desconectado.app.util

import android.content.Context
import com.desconectado.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * API REST de los emuladores de Firebase, para preparar y limpiar el estado de las pruebas
 * instrumentadas. Solo tiene sentido con `-PuseEmulator=true`; se llama fuera del hilo principal.
 */
object EmuladorFirebase {

    private const val PUERTO_AUTH = 9099
    private const val CLAVE_FALSA = "clave-falsa"
    private const val NOMBRE_APP_SIN_RED = "sin-red"
    private const val PUERTO_SIN_SERVICIO = 1

    private val host get() = BuildConfig.EMULATOR_HOST
    private val proyecto get() = FirebaseAuth.getInstance().app.options.projectId
        ?: error("El proyecto de Firebase no tiene projectId")

    /** Elimina todas las cuentas del emulador de Auth. */
    fun limpiarAuth() {
        pedir("DELETE", "http://$host:$PUERTO_AUTH/emulator/v1/projects/$proyecto/accounts")
    }

    /** Elimina una cuenta por su token de identidad, como si se hubiera borrado desde la consola. */
    fun eliminarCuenta(idToken: String) {
        val cuerpo = JSONObject().put("idToken", idToken).toString()
        pedir("POST", "http://$host:$PUERTO_AUTH/identitytoolkit.googleapis.com/v1/accounts:delete?key=$CLAVE_FALSA", cuerpo)
    }

    /** Códigos de restablecimiento de contraseña pendientes en el emulador de Auth. */
    fun codigosRestablecimiento(): List<Pair<String, String>> {
        val respuesta = pedir("GET", "http://$host:$PUERTO_AUTH/emulator/v1/projects/$proyecto/oobCodes")
        val codigos = JSONObject(respuesta).optJSONArray("oobCodes") ?: return emptyList()
        return (0 until codigos.length())
            .map { codigos.getJSONObject(it) }
            .filter { it.optString("requestType") == "PASSWORD_RESET" }
            .map { it.getString("email") to it.getString("oobCode") }
    }

    /** Completa el restablecimiento con el código recibido, como haría el enlace del correo. */
    fun restablecerPassword(codigo: String, passwordNueva: String) {
        val cuerpo = JSONObject().put("oobCode", codigo).put("newPassword", passwordNueva).toString()
        pedir("POST", "http://$host:$PUERTO_AUTH/identitytoolkit.googleapis.com/v1/accounts:resetPassword?key=$CLAVE_FALSA", cuerpo)
    }

    /**
     * Una app de Firebase aparte cuyo Firestore apunta a un puerto sin nada escuchando: tiene el
     * mismo efecto que tener el emulador detenido, sin afectar al resto de las pruebas.
     *
     * Solo sirve para Firestore. NO hacer lo mismo con Auth: en Android la configuración de
     * `FirebaseAuth.useEmulator` es global al proceso y dejaría a todas las pruebas sin servidor.
     */
    fun appSinRed(context: Context): FirebaseApp {
        val app = try {
            FirebaseApp.getInstance(NOMBRE_APP_SIN_RED)
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, NOMBRE_APP_SIN_RED)
        }
        runCatching { FirebaseFirestore.getInstance(app).useEmulator(host, PUERTO_SIN_SERVICIO) }
        return app
    }

    private fun pedir(metodo: String, url: String, cuerpo: String? = null): String {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = 5_000
            conexion.readTimeout = 10_000
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json")
                conexion.outputStream.use { it.write(cuerpo.toByteArray()) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(codigo in 200..299) { "$metodo $url respondió $codigo: $texto" }
            return texto
        } finally {
            conexion.disconnect()
        }
    }
}
