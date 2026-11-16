package com.motomarket.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.motomarket.app.data.firebase.FirebaseCarritoRepository
import com.motomarket.app.data.firebase.FirebaseInformeRepository
import com.motomarket.app.data.firebase.FirebasePedidoRepository
import com.motomarket.app.data.firebase.FirebaseProductoRepository
import com.motomarket.app.data.firebase.FirebaseReporteRepository
import com.motomarket.app.data.firebase.FirebaseServicioRepository
import com.motomarket.app.data.firebase.FirebaseUsuarioRepository

/**
 * Localizador de servicios: único punto que decide qué implementación usa la app.
 * Las pantallas solo conocen las interfaces, así que cambiar de backend no las afecta.
 */
object Repositorios {

    /** True si google-services.json estaba presente al compilar y Firebase se inicializó. */
    fun firebaseConfigurado(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    val usuarios: UsuarioRepository by lazy { FirebaseUsuarioRepository() }
    val productos: ProductoRepository by lazy { FirebaseProductoRepository(usuarios) }
    val carrito: CarritoRepository by lazy { FirebaseCarritoRepository(usuarios) }
    val pedidos: PedidoRepository by lazy { FirebasePedidoRepository(usuarios) }
    val reportes: ReporteRepository by lazy { FirebaseReporteRepository() }
    val servicios: ServicioRepository by lazy { FirebaseServicioRepository(usuarios) }
    val informes: InformeRepository by lazy { FirebaseInformeRepository(usuarios) }
}
