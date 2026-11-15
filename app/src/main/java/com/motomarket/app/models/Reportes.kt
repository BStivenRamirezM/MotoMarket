package com.motomarket.app.models

data class MetricasAdmin(
    val usuariosActivos: Int,
    val productosPublicados: Int,
    val ventasMes: Long,
    val pagosExitososPorcentaje: Int
)

data class VentaMensual(
    val mes: String,
    val valor: Long
)
