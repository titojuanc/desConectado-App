package com.desconectado.app.fakes

import com.desconectado.app.domain.model.Perfil
import com.desconectado.app.domain.model.Resultado
import com.desconectado.app.domain.repository.ProfileRepository

/** Doble de `ProfileRepository` con resultados programables y registro de llamadas. */
class FakeProfileRepository : ProfileRepository {

    var resultadoPerfil: Resultado<Perfil> = Resultado.Exito(Perfil(username = "ana", email = "ana@mail.com"))
    var resultadoAsegurar: Resultado<Unit> = Resultado.Exito(Unit)

    val llamadasPerfil = mutableListOf<String>()
    val llamadasAsegurar = mutableListOf<Triple<String, String?, String>>()

    override suspend fun perfil(uid: String): Resultado<Perfil> {
        llamadasPerfil += uid
        return resultadoPerfil
    }

    override suspend fun asegurarPerfil(uid: String, nombre: String?, email: String): Resultado<Unit> {
        llamadasAsegurar += Triple(uid, nombre, email)
        return resultadoAsegurar
    }
}
