package com.motomarket.app.data

import com.motomarket.app.models.Rol
import com.motomarket.app.models.SolicitudRol
import com.motomarket.app.models.Usuario

/** Autenticación, gestión de perfiles y solicitudes de rol (vendedor o mecánico). */
interface UsuarioRepository {

    /** Inicia sesión y devuelve el perfil; falla si la cuenta está desactivada o sin perfil. */
    suspend fun iniciarSesion(correo: String, password: String): Usuario

    /**
     * Crea la cuenta en Auth y su perfil, siempre con rol comprador. Si llega [solicitud]
     * deja además una solicitud de vendedor o mecánico pendiente de aprobación.
     */
    suspend fun registrar(nombre: String, correo: String, telefono: String, password: String, solicitud: SolicitudRol?): Usuario

    /** El comprador actual pide ser vendedor o mecánico; queda pendiente de aprobación. */
    suspend fun solicitarRol(solicitud: SolicitudRol)

    /** Solicitudes de rol pendientes, de la más antigua a la más reciente (solo administrador). */
    suspend fun obtenerSolicitudesRol(): List<Usuario>

    /** Aprueba (asigna el rol solicitado) o rechaza con [motivo] una solicitud (solo administrador). */
    suspend fun resolverSolicitudRol(usuario: Usuario, aprobar: Boolean, motivo: String)

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
