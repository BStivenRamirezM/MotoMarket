package com.motomarket.app.ui.common

import android.util.Patterns
import android.view.View
import android.widget.EditText

private const val ALPHA_DESHABILITADO = 0.5f
const val LONGITUD_MINIMA_PASSWORD = 8

/** Texto del campo sin espacios al inicio ni al final. */
fun EditText.texto(): String = text.toString().trim()

/** Habilita o deshabilita la vista atenuándola, ya que los botones propios no cambian de color solos. */
fun View.habilitar(habilitado: Boolean) {
    isEnabled = habilitado
    alpha = if (habilitado) 1f else ALPHA_DESHABILITADO
}

/** Iniciales (máximo dos) para el avatar de un usuario. */
fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Valida el formato de un correo electrónico. */
fun esCorreoValido(correo: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(correo).matches()
