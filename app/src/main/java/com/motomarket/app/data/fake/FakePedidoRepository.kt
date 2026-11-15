package com.motomarket.app.data.fake

import com.motomarket.app.data.PedidoRepository
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.ResumenVentas

class FakePedidoRepository : PedidoRepository {

    override suspend fun obtenerPedidosVendedor(): List<Pedido> = DatosEjemplo.pedidosVendedor

    override suspend fun obtenerResumenVentasMes(): ResumenVentas = DatosEjemplo.resumenVentasMes
}
