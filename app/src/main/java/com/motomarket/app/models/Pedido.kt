package com.motomarket.app.models

enum class EstadoPedido { POR_ENVIAR, ENVIADO, ENTREGADO }

data class Pedido(
    val id: Int,
    val producto: String,
    val valor: Int,
    val estado: EstadoPedido
)

data class ResumenVentas(
    val totalMes: Int,
    val cantidadPedidos: Int
)
