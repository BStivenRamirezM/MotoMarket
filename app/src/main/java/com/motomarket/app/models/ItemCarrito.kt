package com.motomarket.app.models

/** Producto dentro del carrito; [agregado] conserva el orden en que se añadió. */
data class ItemCarrito(
    val producto: Producto,
    val cantidad: Int,
    val agregado: Long = 0L
) {
    val subtotal: Int get() = producto.precio * cantidad
}

/** Contenido del carrito con sus totales; el envío solo se cobra si hay productos. */
data class ResumenCarrito(
    val items: List<ItemCarrito>,
    val costoEnvio: Int
) {
    val subtotal: Int get() = items.sumOf { it.subtotal }
    val envio: Int get() = if (items.isEmpty()) 0 else costoEnvio
    val total: Int get() = subtotal + envio
}
