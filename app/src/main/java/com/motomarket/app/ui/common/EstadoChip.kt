package com.motomarket.app.ui.common

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.motomarket.app.R
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.EstadoServicio
import com.motomarket.app.models.Rol

/** Combinación de fondo y color de texto de cada chip de estado. */
enum class TonoChip(@param:DrawableRes val fondo: Int, @param:ColorRes val texto: Int) {
    VERDE(R.drawable.bg_chip_verde, R.color.chip_verde_texto),
    AMARILLO(R.drawable.bg_chip_amarillo, R.color.chip_amarillo_texto),
    ROJO(R.drawable.bg_chip_rojo, R.color.chip_rojo_texto),
    NARANJA(R.drawable.bg_chip_naranja, R.color.chip_naranja_texto)
}

/** Pinta el TextView como chip con la etiqueta y el tono indicados. */
fun TextView.aplicarChip(@StringRes etiqueta: Int, tono: TonoChip) {
    setText(etiqueta)
    setBackgroundResource(tono.fondo)
    setTextColor(ContextCompat.getColor(context, tono.texto))
}

/** Chip de una publicación: pendiente (amarillo), aprobado (verde), rechazado o agotado (rojo). */
fun TextView.mostrarEstado(estado: EstadoProducto) = when (estado) {
    EstadoProducto.PENDIENTE -> aplicarChip(R.string.estado_pendiente, TonoChip.AMARILLO)
    EstadoProducto.APROBADO -> aplicarChip(R.string.estado_aprobado, TonoChip.VERDE)
    EstadoProducto.RECHAZADO -> aplicarChip(R.string.estado_rechazado, TonoChip.ROJO)
    EstadoProducto.AGOTADO -> aplicarChip(R.string.estado_agotado, TonoChip.ROJO)
}

/** Chip de una orden: sin pagar, por enviar (amarillo), enviado (naranja) o entregado (verde). */
fun TextView.mostrarEstado(estado: EstadoPedido) = when (estado) {
    EstadoPedido.PENDIENTE -> aplicarChip(R.string.estado_pendiente_pago, TonoChip.AMARILLO)
    EstadoPedido.PAGADO -> aplicarChip(R.string.estado_por_enviar, TonoChip.AMARILLO)
    EstadoPedido.ENVIADO -> aplicarChip(R.string.estado_enviado, TonoChip.NARANJA)
    EstadoPedido.ENTREGADO -> aplicarChip(R.string.estado_entregado, TonoChip.VERDE)
}

/** Texto del estado de una orden, usado en los diálogos de cambio de estado. */
@StringRes
fun EstadoPedido.etiqueta(): Int = when (this) {
    EstadoPedido.PENDIENTE -> R.string.estado_pendiente_pago
    EstadoPedido.PAGADO -> R.string.estado_por_enviar
    EstadoPedido.ENVIADO -> R.string.estado_enviado
    EstadoPedido.ENTREGADO -> R.string.estado_entregado
}

/** Chip de un servicio técnico según la etapa en la que está. */
fun TextView.mostrarEstado(estado: EstadoServicio) = when (estado) {
    EstadoServicio.SOLICITADO -> aplicarChip(R.string.servicio_estado_solicitado, TonoChip.AMARILLO)
    EstadoServicio.COTIZADO -> aplicarChip(R.string.servicio_estado_cotizado, TonoChip.NARANJA)
    EstadoServicio.PAGADO -> aplicarChip(R.string.servicio_estado_pagado, TonoChip.AMARILLO)
    EstadoServicio.EN_PROCESO -> aplicarChip(R.string.servicio_estado_en_proceso, TonoChip.NARANJA)
    EstadoServicio.TERMINADO -> aplicarChip(R.string.servicio_estado_terminado, TonoChip.VERDE)
    EstadoServicio.RECHAZADO -> aplicarChip(R.string.servicio_estado_rechazado, TonoChip.ROJO)
    EstadoServicio.CANCELADO -> aplicarChip(R.string.servicio_estado_cancelado, TonoChip.ROJO)
}

/** Texto de la etapa de un servicio, usado en los diálogos de avance. */
@StringRes
fun EstadoServicio.etiqueta(): Int = when (this) {
    EstadoServicio.SOLICITADO -> R.string.servicio_estado_solicitado
    EstadoServicio.COTIZADO -> R.string.servicio_estado_cotizado
    EstadoServicio.PAGADO -> R.string.servicio_estado_pagado
    EstadoServicio.EN_PROCESO -> R.string.servicio_estado_en_proceso
    EstadoServicio.TERMINADO -> R.string.servicio_estado_terminado
    EstadoServicio.RECHAZADO -> R.string.servicio_estado_rechazado
    EstadoServicio.CANCELADO -> R.string.servicio_estado_cancelado
}

/** Nombre visible de cada rol. */
@StringRes
fun Rol.etiqueta(): Int = when (this) {
    Rol.COMPRADOR -> R.string.rol_comprador
    Rol.VENDEDOR -> R.string.rol_vendedor
    Rol.MECANICO -> R.string.rol_mecanico
    Rol.ADMIN -> R.string.rol_admin
}
