package com.motomarket.app.models

data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int
) {
    val subtotal: Int get() = producto.precio * cantidad
}

data class Envio(
    val ciudad: String,
    val costo: Int
)

data class ResumenCarrito(
    val items: List<ItemCarrito>,
    val envio: Envio
) {
    val subtotal: Int get() = items.sumOf { it.subtotal }
    val total: Int get() = if (items.isEmpty()) 0 else subtotal + envio.costo
}
