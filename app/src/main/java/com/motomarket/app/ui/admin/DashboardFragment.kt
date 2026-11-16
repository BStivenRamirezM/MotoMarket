package com.motomarket.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.ReporteRepository
import com.motomarket.app.databinding.FragmentDashboardBinding
import com.motomarket.app.databinding.ItemAccesoAdminBinding
import com.motomarket.app.databinding.ItemMetricaBinding
import com.motomarket.app.models.MetricasAdmin
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.util.formatearCOPCompacto
import com.motomarket.app.util.formatearMiles
import kotlinx.coroutines.launch

/** Panel del administrador: 4 métricas calculadas en Firestore y accesos rápidos a cada sección. */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val reporteRepository: ReporteRepository by lazy { Repositorios.reportes }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.accesoUsuarios.configurar(
            R.drawable.ic_group, R.string.drawer_usuarios, R.string.dashboard_usuarios_desc, R.id.nav_usuarios
        )
        binding.accesoProductos.configurar(
            R.drawable.ic_check_circle, R.string.dashboard_productos, R.string.dashboard_productos_desc, R.id.nav_aprobacion
        )
        binding.accesoReportes.configurar(
            R.drawable.ic_bar_chart, R.string.dashboard_reportes, R.string.dashboard_reportes_desc, R.id.nav_reportes
        )
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().intentar { reporteRepository.obtenerMetricas() }?.let { mostrarMetricas(it) }
        }
    }

    /** Formatea las cifras (miles, millones, porcentaje) en las cuatro tarjetas. */
    private fun mostrarMetricas(metricas: MetricasAdmin) {
        binding.metricaUsuarios.mostrar(formatearMiles(metricas.usuariosActivos.toLong()), R.string.metrica_usuarios)
        binding.metricaProductos.mostrar(formatearMiles(metricas.productosPublicados.toLong()), R.string.metrica_productos)
        binding.metricaVentas.mostrar(formatearCOPCompacto(metricas.ventasMes), R.string.metrica_ventas)
        binding.metricaPagos.mostrar(getString(R.string.porcentaje, metricas.pagosExitososPorcentaje), R.string.metrica_pagos)
    }

    /** Escribe la cifra y la etiqueta en una tarjeta de métrica. */
    private fun ItemMetricaBinding.mostrar(cifra: String, @StringRes etiqueta: Int) {
        tvCifra.text = cifra
        tvEtiqueta.setText(etiqueta)
    }

    /** Configura una tarjeta de acceso rápido para que abra [seccion] en el menú lateral. */
    private fun ItemAccesoAdminBinding.configurar(
        @DrawableRes icono: Int,
        @StringRes titulo: Int,
        @StringRes descripcion: Int,
        @IdRes seccion: Int
    ) {
        ivIcono.setImageResource(icono)
        tvTitulo.setText(titulo)
        tvDescripcion.setText(descripcion)
        root.setOnClickListener { (requireActivity() as AdminMainActivity).abrirSeccion(seccion) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
