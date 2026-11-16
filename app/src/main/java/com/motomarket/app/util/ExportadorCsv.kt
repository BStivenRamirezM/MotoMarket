package com.motomarket.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Genera un CSV compatible con Excel en español (separador ";" y BOM UTF-8 para las tildes)
 * y prepara el Intent para compartirlo o abrirlo en otra app.
 */
object ExportadorCsv {

    private const val CARPETA = "informes"
    private const val SEPARADOR = ";"
    private const val BOM = "﻿"

    /** Escribe el archivo en la caché de la app y devuelve un Intent ACTION_SEND con permiso de lectura. */
    suspend fun exportar(context: Context, nombreArchivo: String, encabezado: List<String>, columnas: List<String>, filas: List<List<String>>): Intent =
        withContext(Dispatchers.IO) {
            val carpeta = File(context.cacheDir, CARPETA).apply { mkdirs() }
            val archivo = File(carpeta, "$nombreArchivo.csv")
            val contenido = buildString {
                append(BOM)
                encabezado.forEach { appendLine(celda(it)) }
                appendLine()
                appendLine(columnas.joinToString(SEPARADOR) { celda(it) })
                filas.forEach { fila -> appendLine(fila.joinToString(SEPARADOR) { celda(it) }) }
            }
            archivo.writeText(contenido, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
            Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, nombreArchivo)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    /** Encierra el valor entre comillas y duplica las comillas internas, según el formato CSV. */
    private fun celda(valor: String): String = "\"" + valor.replace("\"", "\"\"") + "\""
}
