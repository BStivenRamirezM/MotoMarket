package com.motomarket.app.ui.common

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.motomarket.app.R

/** Carga una imagen remota con Coil usando el placeholder gris del proyecto y esquinas redondeadas. */
fun ImageView.cargarImagen(url: String) {
    clipToOutline = true
    load(url.ifBlank { null }) {
        placeholder(R.drawable.bg_placeholder_producto)
        error(R.drawable.bg_placeholder_producto)
        fallback(R.drawable.bg_placeholder_producto)
        crossfade(true)
    }
}
