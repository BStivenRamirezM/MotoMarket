package com.motomarket.app.data

import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ResumenVentas

interface PedidoRepository {
    suspend fun obtenerPedidosVendedor(): List<Pedido>
    suspend fun obtenerResumenVentasMes(): ResumenVentas
}
