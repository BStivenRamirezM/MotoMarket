package com.motomarket.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val simbolosColombia = DecimalFormatSymbols(Locale.forLanguageTag("es-CO")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

fun formatearMiles(valor: Long): String = DecimalFormat("#,##0", simbolosColombia).format(valor)

fun formatearCOP(valor: Int): String = "\$" + formatearMiles(valor.toLong())

fun formatearMillones(valor: Long): String = DecimalFormat("0.#", simbolosColombia).format(valor / 1_000_000.0)

fun formatearCOPCompacto(valor: Long): String = "\$" + formatearMillones(valor) + "M"

fun formatearCalificacion(valor: Double): String = DecimalFormat("0.0", simbolosColombia).format(valor)
