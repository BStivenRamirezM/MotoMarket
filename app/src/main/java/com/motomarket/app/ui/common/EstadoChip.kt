package com.motomarket.app.ui.common

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.motomarket.app.R
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Rol

enum class TonoChip(@param:DrawableRes val fondo: Int, @param:ColorRes val texto: Int) {
    VERDE(R.drawable.bg_chip_verde, R.color.chip_verde_texto),
    AMARILLO(R.drawable.bg_chip_amarillo, R.color.chip_amarillo_texto),
    ROJO(R.drawable.bg_chip_rojo, R.color.chip_rojo_texto),
    NARANJA(R.drawable.bg_chip_naranja, R.color.chip_naranja_texto)
}

fun TextView.aplicarChip(@StringRes etiqueta: Int, tono: TonoChip) {
    setText(etiqueta)
    setBackgroundResource(tono.fondo)
    setTextColor(ContextCompat.getColor(context, tono.texto))
}

fun TextView.mostrarEstado(estado: EstadoProducto) = when (estado) {
    EstadoProducto.ACTIVO -> aplicarChip(R.string.estado_activo, TonoChip.VERDE)
    EstadoProducto.AGOTADO -> aplicarChip(R.string.estado_agotado, TonoChip.ROJO)
    EstadoProducto.EN_REVISION -> aplicarChip(R.string.estado_revision, TonoChip.AMARILLO)
    EstadoProducto.PENDIENTE -> aplicarChip(R.string.estado_pendiente, TonoChip.AMARILLO)
    EstadoProducto.APROBADO -> aplicarChip(R.string.estado_aprobado, TonoChip.VERDE)
    EstadoProducto.RECHAZADO -> aplicarChip(R.string.estado_rechazado, TonoChip.ROJO)
}

fun TextView.mostrarEstado(estado: EstadoPedido) = when (estado) {
    EstadoPedido.POR_ENVIAR -> aplicarChip(R.string.estado_por_enviar, TonoChip.AMARILLO)
    EstadoPedido.ENVIADO -> aplicarChip(R.string.estado_enviado, TonoChip.NARANJA)
    EstadoPedido.ENTREGADO -> aplicarChip(R.string.estado_entregado, TonoChip.VERDE)
}

@StringRes
fun Rol.etiqueta(): Int = when (this) {
    Rol.COMPRADOR -> R.string.rol_comprador
    Rol.VENDEDOR -> R.string.rol_vendedor
    Rol.ADMIN -> R.string.rol_admin
}
