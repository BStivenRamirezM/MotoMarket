package com.motomarket.app.data.fake

import com.motomarket.app.data.ReporteRepository
import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.models.VentaMensual

class FakeReporteRepository : ReporteRepository {

    override suspend fun obtenerMetricas(): MetricasAdmin = DatosEjemplo.metricas

    override suspend fun obtenerVentasMensuales(): List<VentaMensual> = DatosEjemplo.ventasMensuales
}
