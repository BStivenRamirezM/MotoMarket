package com.motomarket.app.data

import com.motomarket.app.models.Rol
import com.motomarket.app.models.Usuario

/** Autenticación y gestión de perfiles de usuario. */
interface UsuarioRepository {

    /** Inicia sesión y devuelve el perfil; falla si la cuenta está desactivada o sin perfil. */
    suspend fun iniciarSesion(correo: String, password: String): Usuario

    /**
     * Crea la cuenta en Auth y su perfil, siempre con rol comprador. Si llega [tiendaSolicitada]
     * deja además una solicitud de vendedor pendiente de aprobación.
     */
    suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, tiendaSolicitada: String?): Usuario

    /** El comprador actual pide vender con el nombre de tienda indicado; queda pendiente de aprobación. */
    suspend fun solicitarSerVendedor(nombreTienda: String)

    /** Solicitudes de vendedor pendientes, de la más antigua a la más reciente (solo administrador). */
    suspend fun obtenerSolicitudesVendedor(): List<Usuario>

    /** Aprueba (rol vendedor) o rechaza con [motivo] una solicitud de vendedor (solo administrador). */
    suspend fun resolverSolicitudVendedor(id: String, aprobar: Boolean, motivo: String)

    /** Envía el correo de restablecimiento de contraseña. */
    suspend fun recuperarPassword(correo: String)

    /** Perfil de la sesión guardada por Firebase, validando que siga activa; null si no hay sesión. */
    suspend fun sesionActiva(): Usuario?

    /** Uid del usuario autenticado, o null si no hay sesión. */
    fun uidActual(): String?

    /** Cierra la sesión de Firebase. */
    fun cerrarSesion()

    /** Perfil del usuario autenticado; lanza [SesionRequeridaException] si no hay sesión. */
    suspend fun obtenerUsuarioActual(): Usuario

    /** Guarda una dirección de entrega en el perfil del usuario actual (sin duplicados). */
    suspend fun agregarDireccion(direccion: String)

    /** Lista de usuarios para el administrador, filtrada por nombre o correo. */
    suspend fun buscarUsuarios(texto: String): List<Usuario>

    /** Activa o desactiva una cuenta (solo administrador). */
    suspend fun cambiarEstado(id: String, activo: Boolean)

    /** Cambia el rol de una cuenta (solo administrador). */
    suspend fun cambiarRol(id: String, rol: Rol)

    /** Crea una cuenta desde el panel de administración sin cerrar la sesión del administrador. */
    suspend fun crearUsuario(nombre: String, correo: String, password: String, rol: Rol): Usuario
}
