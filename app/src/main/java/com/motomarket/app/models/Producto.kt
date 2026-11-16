package com.motomarket.app.models

/** Categorías del catálogo; en Firestore se guardan en minúscula (ej. "frenos"). */
enum class Categoria { FRENOS, LLANTAS, CASCOS, ACEITES, TRANSMISION, ACCESORIOS }

/** Ciclo de vida de una publicación: nace pendiente, el admin la aprueba o rechaza y se agota sin stock. */
enum class EstadoProducto { PENDIENTE, APROBADO, RECHAZADO, AGOTADO }

/**
 * Producto publicado por un vendedor (documento de la colección "productos").
 * [fechaRevision] y [motivoRechazo] los llena el administrador al moderar.
 * [imagenBase64] es la foto en JPEG comprimido codificada en Base64 (vacía = placeholder gris).
 */
data class Producto(
    val id: String = "",
    val nombre: String = "",
    val precio: Int = 0,
    val categoria: Categoria = Categoria.ACCESORIOS,
    val marcaModeloCompatible: String = "",
    val cilindraje: String = "",
    val stock: Int = 0,
    val vendedorId: String = "",
    val vendedorNombre: String = "",
    val calificacionVendedor: Double = 0.0,
    val estado: EstadoProducto = EstadoProducto.PENDIENTE,
    val imagenBase64: String = "",
    val fechaCreacion: Long = 0L,
    val fechaRevision: Long = 0L,
    val motivoRechazo: String = ""
)
