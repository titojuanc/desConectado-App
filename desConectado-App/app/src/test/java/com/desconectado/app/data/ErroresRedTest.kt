package com.desconectado.app.data

import com.desconectado.app.domain.model.ErrorApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErroresRedTest {

    @Test
    fun firebaseNetworkExceptionEIOException_sonErroresDeRed() {
        assertTrue(FirebaseNetworkException("sin red").esErrorDeRed())
        assertTrue(IOException("sin red").esErrorDeRed())
    }

    @Test
    fun firestoreUnavailableYDeadlineExceeded_sonErroresDeRed() {
        assertTrue(
            FirebaseFirestoreException("x", FirebaseFirestoreException.Code.UNAVAILABLE).esErrorDeRed(),
        )
        assertTrue(
            FirebaseFirestoreException("x", FirebaseFirestoreException.Code.DEADLINE_EXCEEDED).esErrorDeRed(),
        )
        assertFalse(
            FirebaseFirestoreException("x", FirebaseFirestoreException.Code.PERMISSION_DENIED).esErrorDeRed(),
        )
    }

    @Test
    fun authInternalErrorPorFaltaDeConexion_esErrorDeRed() {
        // Con la protección de reCAPTCHA, Auth informa la falta de red como un error interno.
        val sinRed = FirebaseAuthException(
            "ERROR_INTERNAL_ERROR",
            "An internal error has occurred. [ Failed to connect to /10.0.2.2:1 ]",
        )
        val sinDns = FirebaseAuthException(
            "ERROR_INTERNAL_ERROR",
            "An internal error has occurred. [ Unable to resolve host \"www.googleapis.com\" ]",
        )

        assertTrue(sinRed.esErrorDeRed())
        assertTrue(sinDns.esErrorDeRed())
        assertEquals(ErrorApp.SinConexion, sinRed.aErrorApp())
    }

    @Test
    fun authInternalErrorSinRelacionConLaRed_noEsErrorDeRed() {
        val otro = FirebaseAuthException("ERROR_INTERNAL_ERROR", "An internal error has occurred. [ Bad request ]")

        assertFalse(otro.esErrorDeRed())
        assertEquals(ErrorApp.Desconocido, otro.aErrorApp())
    }

    @Test
    fun erroresDeCredencialesOUsuario_noSonErroresDeRed() {
        assertFalse(FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "mal").esErrorDeRed())
        assertFalse(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe").esErrorDeRed())
    }

    @Test
    fun unaCausaDeRedAnidada_seDetecta() {
        assertTrue(RuntimeException("envoltorio", IOException("sin red")).esErrorDeRed())
        assertFalse(RuntimeException("otro").esErrorDeRed())
    }
}
