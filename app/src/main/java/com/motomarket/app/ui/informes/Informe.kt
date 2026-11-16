package com.motomarket.app.ui.informes

import androidx.annotation.StringRes
import com.motomarket.app.R

/**
 * Informes disponibles. [soloAdmin] restringe los que leen datos de toda la plataforma
 * (servicios técnicos y usuarios); los demás también los usa el vendedor sobre sus propias ventas.
 */
enum class TipoInforme(@param:StringRes val titulo: Int, @param:StringRes val descripcion: Int, val soloAdmin: Boolean) {
    VENTAS(R.string.informe_ventas, R.string.informe_ventas_desc, false),
    MAS_VENDIDOS(R.string.informe_mas_vendidos, R.string.informe_mas_vendidos_desc, false),
    ESTADO_PEDIDOS(R.string.informe_estado_pedidos, R.string.informe_estado_pedidos_desc, false),
    SERVICIOS(R.string.informe_servicios, R.string.informe_servicios_desc, true),
    USUARIOS(R.string.informe_usuarios, R.string.informe_usuarios_desc, true);

    companion object {
        /** Tipos que puede consultar el administrador o el vendedor. */
        fun para(esAdmin: Boolean): List<TipoInforme> = entries.filter { esAdmin || !it.soloAdmin }
    }
}

/** Indicador de resumen: cifra ya formateada y su etiqueta. */
data class Indicador(val valor: String, val etiqueta: String)

/** Resultado listo para mostrar y exportar: indicadores de resumen y una tabla de columnas y filas. */
data class Informe(
    val titulo: String,
    val rango: String,
    val indicadores: List<Indicador>,
    val columnas: List<String>,
    val filas: List<List<String>>
)
