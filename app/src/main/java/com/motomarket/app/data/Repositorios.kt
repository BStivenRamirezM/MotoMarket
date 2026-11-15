package com.motomarket.app.data

import com.motomarket.app.data.fake.FakeCarritoRepository
import com.motomarket.app.data.fake.FakePedidoRepository
import com.motomarket.app.data.fake.FakeProductoRepository
import com.motomarket.app.data.fake.FakeReporteRepository
import com.motomarket.app.data.fake.FakeUsuarioRepository

// Único punto a cambiar para usar implementaciones con Firebase.
object Repositorios {
    val productos: ProductoRepository by lazy { FakeProductoRepository() }
    val usuarios: UsuarioRepository by lazy { FakeUsuarioRepository() }
    val pedidos: PedidoRepository by lazy { FakePedidoRepository() }
    val carrito: CarritoRepository by lazy { FakeCarritoRepository() }
    val reportes: ReporteRepository by lazy { FakeReporteRepository() }
}
