package com.motomarket.app.data

import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.models.VentaMensual

/** Indicadores agregados para el administrador. */
interface ReporteRepository {

    /** Usuarios activos, productos publicados, ventas del mes y % de órdenes pagadas. */
    suspend fun obtenerMetricas(): MetricasAdmin

    /** Total vendido en cada uno de los últimos 6 meses, del más antiguo al actual. */
    suspend fun obtenerVentasMensuales(): List<VentaMensual>
}
