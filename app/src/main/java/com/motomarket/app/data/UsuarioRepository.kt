package com.motomarket.app.data

import com.motomarket.app.models.Rol
import com.motomarket.app.models.Tienda
import com.motomarket.app.models.Usuario

interface UsuarioRepository {
    suspend fun iniciarSesion(correo: String, password: String): Usuario?
    suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, rol: Rol): Usuario
    suspend fun recuperarPassword(correo: String): Boolean
    suspend fun obtenerUsuarioActual(rol: Rol): Usuario
    suspend fun obtenerDirecciones(usuarioId: String): List<String>
    suspend fun obtenerTienda(): Tienda
    suspend fun buscarUsuarios(texto: String): List<Usuario>
    suspend fun cambiarEstado(id: String, activo: Boolean)
    suspend fun crearUsuario(nombre: String, correo: String, rol: Rol): Usuario
}
