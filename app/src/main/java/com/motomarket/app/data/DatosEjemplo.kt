package com.motomarket.app.data

import com.motomarket.app.models.Categoria
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.Producto

/**
 * Semilla opcional con los productos de los mockups. El administrador puede cargarla en Firestore
 * desde el menú lateral ("Cargar datos de ejemplo"); quedan publicados a nombre de su cuenta.
 */
object DatosEjemplo {

    private const val TIENDA_DEMO = "MotoRepuestos JD"

    val productos = listOf(
        Producto(
            nombre = "Pastillas de freno NKD 125", precio = 38_000, categoria = Categoria.FRENOS,
            marcaModeloCompatible = "AKT NKD 125 (2016-2024)", cilindraje = "125 cc", stock = 20,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.APROBADO
        ),
        Producto(
            nombre = "Kit de arrastre FZ 2.0", precio = 145_000, categoria = Categoria.TRANSMISION,
            marcaModeloCompatible = "Yamaha FZ 2.0 / FZN 150 (2014-2024)", cilindraje = "150 cc", stock = 12,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.APROBADO
        ),
        Producto(
            nombre = "Casco certificado DOT", precio = 210_000, categoria = Categoria.CASCOS,
            marcaModeloCompatible = "Universal · Tallas M y L", cilindraje = "Todas", stock = 8,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.APROBADO
        ),
        Producto(
            nombre = "Llanta trasera 130/70-17", precio = 185_000, categoria = Categoria.LLANTAS,
            marcaModeloCompatible = "Pulsar NS 200 / Yamaha FZ / AKT TT", cilindraje = "150 - 200 cc", stock = 4,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.APROBADO
        ),
        Producto(
            nombre = "Espejos NKD originales", precio = 45_000, categoria = Categoria.ACCESORIOS,
            marcaModeloCompatible = "AKT NKD 125", cilindraje = "125 cc", stock = 0,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.AGOTADO
        ),
        Producto(
            nombre = "Cadena reforzada 428H", precio = 68_000, categoria = Categoria.TRANSMISION,
            marcaModeloCompatible = "Motos de 125 a 200 cc", cilindraje = "125 - 200 cc", stock = 15,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.PENDIENTE
        ),
        Producto(
            nombre = "Aceite 20W-50 semisintético", precio = 42_000, categoria = Categoria.ACEITES,
            marcaModeloCompatible = "Motores 4 tiempos", cilindraje = "Todas", stock = 30,
            vendedorNombre = TIENDA_DEMO, calificacionVendedor = 4.8, estado = EstadoProducto.APROBADO
        )
    )
}
