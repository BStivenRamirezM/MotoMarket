package com.motomarket.app.models

enum class Categoria { FRENOS, LLANTAS, CASCOS, ACEITES, TRANSMISION, ACCESORIOS }

enum class EstadoProducto { ACTIVO, AGOTADO, EN_REVISION, PENDIENTE, APROBADO, RECHAZADO }

data class Producto(
    val id: String,
    val nombre: String,
    val precio: Int,
    val categoria: Categoria,
    val marcaModeloCompatible: String,
    val cilindraje: String,
    val stock: Int,
    val vendedor: String,
    val calificacionVendedor: Double,
    val estado: EstadoProducto
)
