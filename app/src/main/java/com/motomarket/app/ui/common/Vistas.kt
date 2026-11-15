package com.motomarket.app.ui.common

import android.view.View
import android.widget.EditText

private const val ALPHA_DESHABILITADO = 0.5f

fun EditText.texto(): String = text.toString().trim()

fun View.habilitar(habilitado: Boolean) {
    isEnabled = habilitado
    alpha = if (habilitado) 1f else ALPHA_DESHABILITADO
}

fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
