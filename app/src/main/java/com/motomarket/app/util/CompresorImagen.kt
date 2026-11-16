package com.motomarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.motomarket.app.data.ImagenNoLegibleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Reduce una foto a JPEG de máximo [BYTES_MAXIMOS] antes de subirla a Firebase Storage. */
object CompresorImagen {

    private const val BYTES_MAXIMOS = 1_000_000
    private const val LADO_MAXIMO = 1600
    private const val CALIDAD_INICIAL = 90
    private const val CALIDAD_MINIMA = 40
    private const val PASO_CALIDAD = 10

    /**
     * Lee la imagen con un submuestreo que evita cargarla completa en memoria, corrige la rotación
     * indicada por EXIF, la escala al lado máximo y baja la calidad JPEG hasta pesar menos de 1 MB.
     */
    suspend fun comprimir(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) throw ImagenNoLegibleException()

        var muestreo = 1
        while (max(limites.outWidth, limites.outHeight) / (muestreo * 2) >= LADO_MAXIMO) muestreo *= 2
        val original = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestreo })
        } ?: throw ImagenNoLegibleException()

        val rotacion = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        val ajustada = escalarYRotar(original, rotacion)

        var calidad = CALIDAD_INICIAL
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { salida ->
                ajustada.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                salida.toByteArray()
            }
            calidad -= PASO_CALIDAD
        } while (bytes.size > BYTES_MAXIMOS && calidad >= CALIDAD_MINIMA)
        bytes
    }

    /** Reduce la imagen al lado máximo y aplica la rotación EXIF en una sola matriz. */
    private fun escalarYRotar(bitmap: Bitmap, rotacion: Int): Bitmap {
        val escala = (LADO_MAXIMO.toFloat() / max(bitmap.width, bitmap.height)).coerceAtMost(1f)
        if (escala == 1f && rotacion == 0) return bitmap
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(rotacion.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
