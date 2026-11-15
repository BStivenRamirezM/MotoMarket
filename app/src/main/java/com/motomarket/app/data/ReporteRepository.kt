package com.motomarket.app.data

import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.models.VentaMensual

interface ReporteRepository {
    suspend fun obtenerMetricas(): MetricasAdmin
    suspend fun obtenerVentasMensuales(): List<VentaMensual>
}
