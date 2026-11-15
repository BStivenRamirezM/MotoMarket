package com.motomarket.app.data.fake

import com.motomarket.app.data.UsuarioRepository
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Tienda
import com.motomarket.app.models.Usuario
import java.util.UUID

class FakeUsuarioRepository : UsuarioRepository {

    private val usuarios = DatosEjemplo.usuarios.toMutableList()

    override suspend fun iniciarSesion(correo: String, password: String): Usuario? =
        (usuarios + DatosEjemplo.administrador).find { it.correo.equals(correo, ignoreCase = true) && it.activo }

    override suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, rol: Rol): Usuario =
        crearUsuario(nombre, correo, rol)

    override suspend fun recuperarPassword(correo: String): Boolean = true

    override suspend fun obtenerUsuarioActual(rol: Rol): Usuario = when (rol) {
        Rol.ADMIN -> DatosEjemplo.administrador
        else -> usuarios.first { it.rol == rol }
    }

    override suspend fun obtenerDirecciones(usuarioId: String): List<String> = DatosEjemplo.direccionesComprador

    override suspend fun obtenerTienda(): Tienda = DatosEjemplo.tienda

    override suspend fun buscarUsuarios(texto: String): List<Usuario> =
        usuarios.filter {
            texto.isBlank() || it.nombre.contains(texto, ignoreCase = true) || it.correo.contains(texto, ignoreCase = true)
        }

    override suspend fun cambiarEstado(id: String, activo: Boolean) {
        val indice = usuarios.indexOfFirst { it.id == id }
        if (indice >= 0) usuarios[indice] = usuarios[indice].copy(activo = activo)
    }

    override suspend fun crearUsuario(nombre: String, correo: String, rol: Rol): Usuario =
        Usuario(UUID.randomUUID().toString(), nombre, correo, rol, true).also { usuarios.add(it) }
}
