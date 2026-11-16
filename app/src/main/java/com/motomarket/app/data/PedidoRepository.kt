package com.motomarket.app.data

import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ResumenCarrito
import com.motomarket.app.models.ResumenVentas

/** Órdenes de compra: creación tras el pago, historial y seguimiento de envíos. */
interface PedidoRepository {

    /**
     * Crea la orden pagada y descuenta el stock en una sola transacción; falla con
     * [StockInsuficienteException] si algún producto ya no alcanza.
     */
    suspend fun confirmarCompra(
        resumen: ResumenCarrito,
        direccion: String,
        lat: Double?,
        lng: Double?,
        metodoPago: String,
        refPago: String
    ): Pedido

    /** Órdenes del comprador actual, de la más reciente a la más antigua. */
    suspend fun obtenerHistorialComprador(): List<Pedido>

    /** Órdenes que contienen productos del vendedor actual. */
    suspend fun obtenerPedidosVendedor(): List<Pedido>

    /** Ventas del mes del vendedor actual (solo órdenes pagadas). */
    suspend fun obtenerResumenVentasMes(): ResumenVentas

    /** Avanza el estado de envío de una orden. */
    suspend fun cambiarEstado(id: String, estado: EstadoPedido)
}
