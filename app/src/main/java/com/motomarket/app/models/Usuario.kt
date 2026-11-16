package com.motomarket.app.models

/** Rol que define a qué módulo entra el usuario; en Firestore: "comprador", "vendedor" o "admin". */
enum class Rol { COMPRADOR, VENDEDOR, ADMIN }

/**
 * Perfil de un usuario (documento "usuarios/{uid}"). Para los vendedores, [nombre] es el nombre
 * de la tienda y [calificacion] su promedio; [direcciones] guarda las direcciones de entrega usadas.
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
    val direcciones: List<String> = emptyList()
)
