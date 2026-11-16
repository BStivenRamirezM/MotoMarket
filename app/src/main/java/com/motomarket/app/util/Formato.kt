package com.motomarket.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utilidades para mostrar cifras y fechas con el formato colombiano. */
private val localeColombia = Locale.forLanguageTag("es-CO")

private val simbolosColombia = DecimalFormatSymbols(localeColombia).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

/** 1248 → "1.248". */
fun formatearMiles(valor: Long): String = DecimalFormat("#,##0", simbolosColombia).format(valor)

/** 145000 → "$145.000". */
fun formatearCOP(valor: Int): String = "\$" + formatearMiles(valor.toLong())

/** 18400000 → "18,4" (millones). */
fun formatearMillones(valor: Long): String = DecimalFormat("0.#", simbolosColombia).format(valor / 1_000_000.0)

/** 18400000 → "$18,4M". */
fun formatearCOPCompacto(valor: Long): String = "\$" + formatearMillones(valor) + "M"

/** 4.8 → "4,8". */
fun formatearCalificacion(valor: Double): String = DecimalFormat("0.0", simbolosColombia).format(valor)

/** Milisegundos → "05/10/2026". */
fun formatearFecha(milisegundos: Long): String = SimpleDateFormat("dd/MM/yyyy", localeColombia).format(Date(milisegundos))

/** Milisegundos → "05/10". */
fun formatearFechaCorta(milisegundos: Long): String = SimpleDateFormat("dd/MM", localeColombia).format(Date(milisegundos))
