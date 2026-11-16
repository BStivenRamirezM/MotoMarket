package com.motomarket.app.data.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.firestore
import com.motomarket.app.data.ReporteRepository
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.VentaMensual
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Métricas del administrador: conteos con agregaciones de Firestore y sumas sobre las órdenes. */
class FirebaseReporteRepository : ReporteRepository {

    private val db = Firebase.firestore

    override suspend fun obtenerMetricas(): MetricasAdmin {
        val usuariosActivos = db.collection(Colecciones.USUARIOS).whereEqualTo("activo", true)
            .count().get(AggregateSource.SERVER).await().count
        val productosPublicados = db.collection(Colecciones.PRODUCTOS)
            .whereEqualTo("estado", EstadoProducto.APROBADO.aTexto())
            .count().get(AggregateSource.SERVER).await().count
        val pedidosDelMes = pedidosDesde(inicioDeMes(0))
        val pagados = pedidosDelMes.filter { it.estado != EstadoPedido.PENDIENTE }
        val porcentaje = if (pedidosDelMes.isEmpty()) 0 else pagados.size * 100 / pedidosDelMes.size
        return MetricasAdmin(
            usuariosActivos = usuariosActivos.toInt(),
            productosPublicados = productosPublicados.toInt(),
            ventasMes = pagados.sumOf { it.total.toLong() },
            pagosExitososPorcentaje = porcentaje
        )
    }

    override suspend fun obtenerVentasMensuales(): List<VentaMensual> {
        val pagados = pedidosDesde(inicioDeMes(MESES_REPORTE - 1)).filter { it.estado != EstadoPedido.PENDIENTE }
        val formatoMes = SimpleDateFormat("MMM", Locale.forLanguageTag("es-CO"))
        return (MESES_REPORTE - 1 downTo 0).map { mesesAtras ->
            val inicio = inicioDeMes(mesesAtras)
            val fin = inicioDeMes(mesesAtras - 1)
            VentaMensual(
                mes = formatoMes.format(inicio).replace(".", "").replaceFirstChar { it.uppercase() },
                valor = pagados.filter { it.fecha in inicio until fin }.sumOf { it.total.toLong() }
            )
        }
    }

    private suspend fun pedidosDesde(fecha: Long): List<Pedido> =
        db.collection(Colecciones.PEDIDOS).whereGreaterThanOrEqualTo("fecha", fecha).get().await()
            .documents.map { it.aPedido() }

    /** Milisegundos del primer instante del mes ubicado [mesesAtras] meses antes del actual (negativo = futuro). */
    private fun inicioDeMes(mesesAtras: Int): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, -mesesAtras)
    }.timeInMillis

    private companion object {
        const val MESES_REPORTE = 6
    }
}
