package com.motomarket.app.ui.admin

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.data.ReporteRepository
import com.motomarket.app.databinding.FragmentReporteVentasBinding
import com.motomarket.app.databinding.ItemBarraGraficoBinding
import com.motomarket.app.models.VentaMensual
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.ui.common.mostrarMensaje
import com.motomarket.app.util.formatearMillones
import kotlinx.coroutines.launch

/** Reportes: gráfico de barras con las ventas reales de los últimos 6 meses y accesos de auditoría/exportación. */
class ReporteVentasFragment : Fragment() {

    private var _binding: FragmentReporteVentasBinding? = null
    private val binding get() = _binding!!

    private val reporteRepository: ReporteRepository by lazy { Repositorios.reportes }
    private val tonosBarras = listOf(
        R.color.naranja_claro, R.color.naranja_claro, R.color.naranja_medio,
        R.color.naranja_medio, R.color.naranja_primario, R.color.naranja_oscuro
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReporteVentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardAuditoria.setOnClickListener { requireContext().mostrarMensaje(R.string.reportes_auditoria_msg) }
        binding.cardExportar.setOnClickListener { requireContext().mostrarMensaje(R.string.reportes_exportar_msg) }
        viewLifecycleOwner.lifecycleScope.launch {
            requireContext().intentar { reporteRepository.obtenerVentasMensuales() }?.let { dibujarGrafico(it) }
        }
    }

    /** Dibuja una barra por mes con altura proporcional al mes de mayor venta. */
    private fun dibujarGrafico(ventas: List<VentaMensual>) {
        val alturaMaxima = resources.getDimensionPixelSize(R.dimen.grafico_barra_altura_max)
        val valorMaximo = (ventas.maxOfOrNull { it.valor } ?: return).coerceAtLeast(1L)
        binding.contenedorBarras.removeAllViews()

        ventas.forEachIndexed { indice, venta ->
            val barra = ItemBarraGraficoBinding.inflate(layoutInflater, binding.contenedorBarras, false)
            barra.tvValor.text = formatearMillones(venta.valor)
            barra.tvMes.text = venta.mes
            barra.vBarra.layoutParams.height = (alturaMaxima * venta.valor / valorMaximo).toInt()
            val tono = tonosBarras[indice.coerceAtMost(tonosBarras.lastIndex)]
            barra.vBarra.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), tono))
            binding.contenedorBarras.addView(barra.root)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
