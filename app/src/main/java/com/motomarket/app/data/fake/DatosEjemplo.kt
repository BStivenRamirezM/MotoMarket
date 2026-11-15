package com.motomarket.app.data.fake

import com.motomarket.app.models.Categoria
import com.motomarket.app.models.Envio
import com.motomarket.app.models.EstadoPedido
import com.motomarket.app.models.EstadoProducto
import com.motomarket.app.models.ItemCarrito
import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.models.Moderacion
import com.motomarket.app.models.Pedido
import com.motomarket.app.models.Producto
import com.motomarket.app.models.ResumenVentas
import com.motomarket.app.models.Rol
import com.motomarket.app.models.Tienda
import com.motomarket.app.models.Usuario
import com.motomarket.app.models.VentaMensual

object DatosEjemplo {

    private const val TIENDA_DEMO = "MotoRepuestos JD"

    val productos = listOf(
        Producto(
            "p1", "Pastillas de freno NKD 125", 38_000, Categoria.FRENOS,
            "AKT NKD 125 (2016-2024)", "125 cc", 20,
            "Frenos y Partes Bogotá", 4.6, EstadoProducto.ACTIVO
        ),
        Producto(
            "p2", "Kit de arrastre FZ 2.0", 145_000, Categoria.TRANSMISION,
            "Yamaha FZ 2.0 / FZN 150 (2014-2024)", "150 cc", 12,
            TIENDA_DEMO, 4.8, EstadoProducto.ACTIVO
        ),
        Producto(
            "p3", "Casco certificado DOT", 210_000, Categoria.CASCOS,
            "Universal · Tallas M y L", "Todas", 8,
            "Cascos Andinos", 4.9, EstadoProducto.ACTIVO
        ),
        Producto(
            "p4", "Llanta trasera 130/70-17", 185_000, Categoria.LLANTAS,
            "Pulsar NS 200 / Yamaha FZ / AKT TT", "150 - 200 cc", 4,
            TIENDA_DEMO, 4.8, EstadoProducto.ACTIVO
        ),
        Producto(
            "p5", "Espejos NKD originales", 45_000, Categoria.ACCESORIOS,
            "AKT NKD 125", "125 cc", 0,
            TIENDA_DEMO, 4.8, EstadoProducto.AGOTADO
        ),
        Producto(
            "p6", "Cadena reforzada 428H", 68_000, Categoria.TRANSMISION,
            "Motos de 125 a 200 cc", "125 - 200 cc", 15,
            TIENDA_DEMO, 4.8, EstadoProducto.EN_REVISION
        ),
        Producto(
            "p7", "Aceite 20W-50 semisintético", 42_000, Categoria.ACEITES,
            "Motores 4 tiempos", "Todas", 30,
            "Lubricantes del Sur", 4.7, EstadoProducto.ACTIVO
        )
    )

    val carrito = listOf(
        ItemCarrito(productos[1], 1),
        ItemCarrito(productos[0], 2)
    )

    val envio = Envio("Soacha", 9_000)

    val pedidosVendedor = listOf(
        Pedido(1042, "Kit arrastre", 145_000, EstadoPedido.POR_ENVIAR),
        Pedido(1038, "Llanta 130/70", 185_000, EstadoPedido.ENVIADO),
        Pedido(1031, "Pastillas freno", 76_000, EstadoPedido.ENTREGADO)
    )

    val resumenVentasMes = ResumenVentas(1_250_000, 14)

    val usuarios = listOf(
        Usuario("u1", "Carlos Pérez", "carlos.perez@correo.co", Rol.VENDEDOR, true),
        Usuario("u2", "Laura Gómez", "laura.gomez@correo.co", Rol.COMPRADOR, true),
        Usuario("u3", "Andrés Rojas", "andres.rojas@correo.co", Rol.VENDEDOR, false),
        Usuario("u4", "Diana León", "diana.leon@correo.co", Rol.COMPRADOR, true)
    )

    val administrador = Usuario("u0", "Administrador", "admin@motomarket.co", Rol.ADMIN, true)

    val direccionesComprador = listOf(
        "Cra 7 # 12-34, Soacha · Casa",
        "Cl 45 # 10-20, Bogotá · Trabajo"
    )

    val tienda = Tienda(TIENDA_DEMO, "Carlos Pérez", 4.8, "Soacha, Cundinamarca", "ventas@motorepuestosjd.co")

    val moderaciones = listOf(
        Moderacion("m1", "Cadena reforzada 428H", "MotoRep JD", EstadoProducto.PENDIENTE),
        Moderacion("m2", "Casco sin certificación", "Cascos Express", EstadoProducto.RECHAZADO, motivo = "normativa"),
        Moderacion("m3", "Kit arrastre FZ", "MotoRep JD", EstadoProducto.APROBADO, fecha = "05/10")
    )

    val metricas = MetricasAdmin(1_248, 356, 18_400_000, 97)

    val ventasMensuales = listOf(
        VentaMensual("May", 9_800_000),
        VentaMensual("Jun", 11_200_000),
        VentaMensual("Jul", 12_900_000),
        VentaMensual("Ago", 14_600_000),
        VentaMensual("Sep", 16_300_000),
        VentaMensual("Oct", 18_400_000)
    )
}
