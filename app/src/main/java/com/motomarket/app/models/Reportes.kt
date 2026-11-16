package com.motomarket.app.models

/** Indicadores del panel de administración calculados a partir de Firestore. */
data class MetricasAdmin(
    val usuariosActivos: Int,
    val productosPublicados: Int,
    val ventasMes: Long,
    val pagosExitososPorcentaje: Int
)

/** Total vendido en un mes, usado por el gráfico de barras de reportes. */
data class VentaMensual(
    val mes: String,
    val valor: Long
)
