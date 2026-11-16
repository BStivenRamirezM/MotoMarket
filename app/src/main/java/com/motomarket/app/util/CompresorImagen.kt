package com.motomarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.motomarket.app.data.ImagenMuyGrandeException
import com.motomarket.app.data.ImagenNoLegibleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reduce una foto a un JPEG pequeño (~[BYTES_OBJETIVO], lado mayor de [LADO_MAXIMO] px) para guardarla
 * en Base64 dentro del documento del producto, que en Firestore no puede pasar de 1 MB.
 */
object CompresorImagen {

    /** Peso buscado: en Base64 queda en ~270 KB, lejos del límite de 1 MB por documento. */
    const val BYTES_OBJETIVO = 200_000

    /** Peso máximo aceptado tras la compresión más agresiva (~670 KB en Base64). */
    const val BYTES_MAXIMOS = 500_000

    private const val LADO_MAXIMO = 800
    private const val LADO_MINIMO = 320
    private const val CALIDAD_INICIAL = 85
    private const val CALIDAD_MINIMA = 30
    private const val PASO_CALIDAD = 10
    private const val FACTOR_REDUCCION = 0.75f

    /**
     * Lee la imagen con submuestreo (no la carga completa en memoria), corrige la rotación EXIF, la
     * escala a [LADO_MAXIMO] y baja la calidad JPEG hasta [BYTES_OBJETIVO]. Si aun así pesa más, reduce
     * las dimensiones y vuelve a intentar. Lanza [ImagenMuyGrandeException] si no logra bajar de [BYTES_MAXIMOS].
     */
    suspend fun comprimir(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.Default) {
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

        var lado = LADO_MAXIMO
        var mejor: ByteArray
        while (true) {
            mejor = comprimirConCalidad(escalarYRotar(original, rotacion, lado))
            if (mejor.size <= BYTES_OBJETIVO || lado <= LADO_MINIMO) break
            lado = (lado * FACTOR_REDUCCION).roundToInt()
        }
        if (mejor.size > BYTES_MAXIMOS) throw ImagenMuyGrandeException()
        mejor
    }

    /** Baja la calidad JPEG de 10 en 10 hasta llegar al objetivo o a la calidad mínima. */
    private fun comprimirConCalidad(bitmap: Bitmap): ByteArray {
        var calidad = CALIDAD_INICIAL
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { salida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                salida.toByteArray()
            }
            calidad -= PASO_CALIDAD
        } while (bytes.size > BYTES_OBJETIVO && calidad >= CALIDAD_MINIMA)
        return bytes
    }

    /** Reduce la imagen a [lado] px en su lado mayor y aplica la rotación EXIF en una sola matriz. */
    private fun escalarYRotar(bitmap: Bitmap, rotacion: Int, lado: Int): Bitmap {
        val escala = (lado.toFloat() / max(bitmap.width, bitmap.height)).coerceAtMost(1f)
        if (escala == 1f && rotacion == 0) return bitmap
        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(rotacion.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
