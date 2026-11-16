package com.motomarket.app.models

/**
 * Ciclo de un servicio técnico: el comprador lo solicita, el mecánico cotiza, el comprador paga y el
 * mecánico lo lleva a "en proceso" y "terminado". Puede cerrarse antes como rechazado o cancelado.
 */
enum class EstadoServicio {
    SOLICITADO, COTIZADO, PAGADO, EN_PROCESO, TERMINADO, RECHAZADO, CANCELADO;

    /** Siguiente estado que puede asignar el mecánico después del pago, o null si no aplica. */
    fun siguiente(): EstadoServicio? = when (this) {
        PAGADO -> EN_PROCESO
        EN_PROCESO -> TERMINADO
        else -> null
    }

    /** True mientras el mecánico todavía puede editar la cotización. */
    val cotizable: Boolean get() = this == SOLICITADO || this == COTIZADO
}

/** Línea de la cotización que define el mecánico: cantidad y precio unitario libres. */
data class LineaServicio(
    val descripcion: String = "",
    val cantidad: Int = 1,
    val precioUnitario: Int = 0
) {
    val subtotal: Int get() = cantidad * precioUnitario
}

/**
 * Solicitud de servicio técnico (colección "servicios"). Las [lineas] y el [total] solo los escribe el
 * mecánico asignado; el comprador únicamente acepta (paga) o rechaza la cotización.
 */
data class ServicioTecnico(
    val id: String = "",
    val compradorId: String = "",
    val compradorNombre: String = "",
    val mecanicoId: String = "",
    val mecanicoNombre: String = "",
    val moto: String = "",
    val descripcionFalla: String = "",
    val lineas: List<LineaServicio> = emptyList(),
    val total: Int = 0,
    val estado: EstadoServicio = EstadoServicio.SOLICITADO,
    val notaMecanico: String = "",
    val metodoPago: String = "",
    val refPago: String = "",
    val fechaSolicitud: Long = 0L,
    val fechaPago: Long = 0L,
    val fechaActualizacion: Long = 0L
) {
    /** Número corto y legible del servicio, derivado del id del documento. */
    val numero: String get() = id.take(8).uppercase()
}

/** Perfil público de un mecánico aprobado (colección "mecanicos"), visible para los compradores. */
data class Mecanico(
    val id: String = "",
    val nombre: String = "",
    val nombreTaller: String = "",
    val telefono: String = "",
    val calificacion: Double = 0.0,
    val activo: Boolean = true
)
