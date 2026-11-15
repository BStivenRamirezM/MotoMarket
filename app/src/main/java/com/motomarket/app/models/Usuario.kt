package com.motomarket.app.models

enum class Rol { COMPRADOR, VENDEDOR, ADMIN }

data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val activo: Boolean
)

data class Tienda(
    val nombre: String,
    val propietario: String,
    val calificacion: Double,
    val ciudad: String,
    val correo: String
)
