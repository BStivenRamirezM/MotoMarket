package com.motomarket.app.models

/** Estados de una orden; el vendedor la avanza de PAGADO ("Por enviar") a ENVIADO y ENTREGADO. */
enum class EstadoPedido {
    PENDIENTE, PAGADO, ENVIADO, ENTREGADO;

    /** Siguiente estado que puede asignar el vendedor, o null si ya no hay más avances. */
    fun siguiente(): EstadoPedido? = when (this) {
        PAGADO -> ENVIADO
        ENVIADO -> ENTREGADO
        else -> null
    }
}

/** Producto comprado dentro de una orden; guarda precio y nombre del momento de la compra. */
data class ItemPedido(
    val productoId: String = "",
    val nombre: String = "",
    val precio: Int = 0,
    val cantidad: Int = 0,
    val vendedorId: String = ""
) {
    val subtotal: Int get() = precio * cantidad
}

/**
 * Orden de compra (colección "pedidos"). [vendedorIds] permite que cada vendedor consulte
 * las órdenes que contienen sus productos con una sola consulta array-contains.
 */
data class Pedido(
    val id: String = "",
    val compradorId: String = "",
    val compradorNombre: String = "",
    val items: List<ItemPedido> = emptyList(),
    val vendedorIds: List<String> = emptyList(),
    val subtotal: Int = 0,
    val envio: Int = 0,
    val total: Int = 0,
    val direccionEntrega: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val metodoPago: String = "",
    val estado: EstadoPedido = EstadoPedido.PENDIENTE,
    val refPago: String = "",
    val fecha: Long = 0L
) {
    /** Número corto y legible de la orden, derivado del id del documento. */
    val numero: String get() = id.take(8).uppercase()

    /** Ítems de la orden que pertenecen a un vendedor concreto. */
    fun itemsDeVendedor(vendedorId: String): List<ItemPedido> = items.filter { it.vendedorId == vendedorId }
}

/** Totales del mes para el banner de pedidos del vendedor. */
data class ResumenVentas(
    val totalMes: Int,
    val cantidadPedidos: Int
)
