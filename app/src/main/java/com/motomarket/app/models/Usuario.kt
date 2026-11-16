package com.motomarket.app.models

/** Rol que define a qué módulo entra el usuario; en Firestore: "comprador", "vendedor" o "admin". */
enum class Rol { COMPRADOR, VENDEDOR, ADMIN }

/**
 * Estado de la solicitud de un comprador para vender. Nadie se vuelve vendedor por sí mismo:
 * la solicitud queda pendiente hasta que un administrador la aprueba o la rechaza.
 */
enum class EstadoSolicitud { NINGUNA, PENDIENTE, APROBADA, RECHAZADA }

/**
 * Perfil de un usuario (documento "usuarios/{uid}"). [nombreTienda] es el nombre con el que
 * vende (si lo tiene), [calificacion] su promedio como vendedor y [direcciones] las de entrega usadas.
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
    val nombreTienda: String = "",
    val solicitudVendedor: EstadoSolicitud = EstadoSolicitud.NINGUNA,
    val motivoSolicitud: String = "",
    val fechaSolicitud: Long = 0L
) {
    /** Nombre que se muestra en el catálogo: el de la tienda o, si no tiene, el de la persona. */
    val nombreVendedor: String get() = nombreTienda.ifBlank { nombre }
}
