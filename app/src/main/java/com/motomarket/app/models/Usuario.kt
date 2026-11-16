package com.motomarket.app.models

/** Rol que define a qué módulo entra el usuario; en Firestore: "comprador", "vendedor", "mecanico" o "admin". */
enum class Rol { COMPRADOR, VENDEDOR, MECANICO, ADMIN }

/**
 * Estado de la solicitud de un comprador para convertirse en vendedor o mecánico. Nadie cambia de rol
 * por sí mismo: la solicitud queda pendiente hasta que un administrador la aprueba o la rechaza.
 */
enum class EstadoSolicitud { NINGUNA, PENDIENTE, APROBADA, RECHAZADA }

/** Lo que pide un comprador: el rol (vendedor o mecánico) y el nombre de su tienda o taller. */
data class SolicitudRol(
    val rol: Rol,
    val nombreNegocio: String
)

/**
 * Perfil de un usuario (documento "usuarios/{uid}"). [nombreNegocio] es el nombre de su tienda o taller,
 * [calificacion] su promedio como vendedor o mecánico y [direcciones] las de entrega usadas.
 */
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val rol: Rol = Rol.COMPRADOR,
    val activo: Boolean = true,
    val fechaRegistro: Long = 0L,
    val calificacion: Double = 0.0,
    val direcciones: List<String> = emptyList(),
    val nombreNegocio: String = "",
    val rolSolicitado: Rol = Rol.VENDEDOR,
    val solicitudRol: EstadoSolicitud = EstadoSolicitud.NINGUNA,
    val motivoSolicitud: String = "",
    val fechaSolicitud: Long = 0L
) {
    /** Nombre público: el de la tienda o taller o, si no tiene, el de la persona. */
    val nombrePublico: String get() = nombreNegocio.ifBlank { nombre }
}
