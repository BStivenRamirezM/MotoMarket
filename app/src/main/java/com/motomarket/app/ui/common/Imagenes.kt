package com.motomarket.app.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder
import com.motomarket.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Carga una imagen local (foto recién tomada o elegida) con Coil, con el placeholder gris del proyecto. */
fun ImageView.cargarImagen(uri: String) {
    clipToOutline = true
    load(uri.ifBlank { null }) {
        placeholder(R.drawable.bg_placeholder_producto)
        error(R.drawable.bg_placeholder_producto)
        fallback(R.drawable.bg_placeholder_producto)
        crossfade(true)
    }
}

/**
 * Muestra la foto de un producto guardada en Base64 en Firestore. La decodificación se hace fuera del
 * hilo principal y con submuestreo al tamaño de la vista; los bitmaps ya decodificados se guardan en
 * memoria para que el scroll de las listas sea fluido. Sin imagen se deja el placeholder gris.
 */
fun ImageView.cargarImagenBase64(base64: String) {
    clipToOutline = true
    (getTag(R.id.tag_carga_imagen) as? Job)?.cancel()
    setTag(R.id.tag_carga_imagen, null)
    if (base64.isBlank()) return setImageResource(R.drawable.bg_placeholder_producto)

    val clave = "${base64.length}:${base64.hashCode()}"
    CacheImagenes.memoria.get(clave)?.let { return setImageBitmap(it) }

    setImageResource(R.drawable.bg_placeholder_producto)
    val dueno = findViewTreeLifecycleOwner() ?: context.buscarLifecycleOwner() ?: return
    val ladoDeseado = max(width, resources.displayMetrics.widthPixels / 2)
    val trabajo = dueno.lifecycleScope.launch {
        val bitmap = withContext(Dispatchers.Default) { decodificarBase64(base64, ladoDeseado) } ?: return@launch
        CacheImagenes.memoria.put(clave, bitmap)
        setImageBitmap(bitmap)
    }
    setTag(R.id.tag_carga_imagen, trabajo)
}

/** Decodifica el Base64 a Bitmap leyendo primero las dimensiones para no cargar más píxeles de los necesarios. */
private fun decodificarBase64(base64: String, ladoDeseado: Int): Bitmap? = runCatching {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
    var muestreo = 1
    while (max(limites.outWidth, limites.outHeight) / (muestreo * 2) >= ladoDeseado) muestreo *= 2
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestreo })
}.getOrNull()

/** Recorre los ContextWrapper hasta la Activity, que es dueña del ciclo de vida. */
private fun Context.buscarLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.buscarLifecycleOwner()
    else -> null
}

/** Caché en memoria de las fotos decodificadas: hasta 1/8 de la memoria disponible para la app. */
private object CacheImagenes {
    val memoria = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }
}
