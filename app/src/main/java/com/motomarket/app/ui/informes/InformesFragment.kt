package com.motomarket.app.ui.informes

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.motomarket.app.R
import com.motomarket.app.data.Repositorios
import com.motomarket.app.databinding.FragmentInformesBinding
import com.motomarket.app.databinding.ItemCeldaInformeBinding
import com.motomarket.app.databinding.ItemMetricaBinding
import com.motomarket.app.ui.common.conCarga
import com.motomarket.app.ui.common.intentar
import com.motomarket.app.util.ExportadorCsv
import com.motomarket.app.util.formatearFecha
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/**
 * Informes con filtros: el usuario elige qué consultar (ventas, más vendidos, estado de pedidos y, si es
 * admin, servicios técnicos y usuarios) y el período. Muestra indicadores y una tabla exportable a CSV/Excel.
 */
class InformesFragment : Fragment() {

    private var _binding: FragmentInformesBinding? = null
    private val binding get() = _binding!!

    private val esAdmin by lazy { requireArguments().getBoolean(ARG_ADMIN) }
    private var tipoElegido = TipoInforme.VENTAS
    private var rangoPersonalizado: Pair<Long, Long>? = null
    private var ultimoInforme: Informe? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInformesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.isVisible = requireArguments().getBoolean(ARG_BARRA)
        crearChipsDeTipo()
        binding.chipsPeriodo.setOnCheckedStateChangeListener { _, ids ->
            if (ids.firstOrNull() == R.id.chipPersonalizado) elegirRangoPersonalizado() else mostrarRango()
        }
        binding.btnGenerar.setOnClickListener { generar() }
        binding.btnExportar.setOnClickListener { exportar() }
        mostrarRango()
    }

    /** Un chip por cada informe permitido para el rol; el primero queda seleccionado. */
    private fun crearChipsDeTipo() {
        TipoInforme.para(esAdmin).forEachIndexed { indice, tipo ->
            val chip = layoutInflater.inflate(R.layout.item_chip_filtro, binding.chipsTipo, false) as Chip
            chip.id = View.generateViewId()
            chip.setText(tipo.titulo)
            chip.isChecked = indice == 0
            chip.setOnClickListener {
                tipoElegido = tipo
                binding.tvDescripcionTipo.setText(tipo.descripcion)
            }
            binding.chipsTipo.addView(chip)
        }
        binding.tvDescripcionTipo.setText(tipoElegido.descripcion)
    }

    /** Selector de rango de Material; MaterialDatePicker entrega fechas en UTC y aquí se pasan a días locales. */
    private fun elegirRangoPersonalizado() {
        val selector = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.informes_elige_rango)
            .build()
        selector.addOnPositiveButtonClickListener { seleccion ->
            rangoPersonalizado = inicioDelDia(seleccion.first) to finDelDia(seleccion.second)
            mostrarRango()
        }
        selector.addOnNegativeButtonClickListener { if (rangoPersonalizado == null) binding.chipsPeriodo.check(R.id.chipEsteMes) }
        selector.show(childFragmentManager, "rango")
    }

    /** Rango [desde, hasta] en milisegundos según el chip de período seleccionado. */
    private fun rangoActual(): Pair<Long, Long> {
        val ahora = System.currentTimeMillis()
        val inicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return when (binding.chipsPeriodo.checkedChipId) {
            R.id.chip30Dias -> inicio.apply { add(Calendar.DAY_OF_YEAR, -29) }.timeInMillis to ahora
            R.id.chip3Meses -> inicio.apply { add(Calendar.MONTH, -3) }.timeInMillis to ahora
            R.id.chipEsteAnio -> inicio.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis to ahora
            R.id.chipPersonalizado -> rangoPersonalizado ?: (inicio.timeInMillis to ahora)
            else -> inicio.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis to ahora
        }
    }

    /** Muestra bajo los chips las fechas exactas del período elegido. */
    private fun mostrarRango() {
        val (desde, hasta) = rangoActual()
        binding.tvRango.text = getString(R.string.informe_rango, formatearFecha(desde), formatearFecha(hasta))
    }

    /** Consulta Firestore y construye el informe; el vendedor solo ve sus propias ventas. */
    private fun generar() {
        val (desde, hasta) = rangoActual()
        val vendedorId = if (esAdmin) null else Repositorios.usuarios.uidActual()
        val constructor = ConstructorInforme(requireContext(), Repositorios.informes, vendedorId)
        viewLifecycleOwner.lifecycleScope.launch {
            val informe = requireContext().conCarga(binding.progreso, binding.btnGenerar) {
                constructor.construir(tipoElegido, desde, hasta)
            } ?: return@launch
            ultimoInforme = informe
            mostrarInforme(informe)
        }
    }

    /** Pinta el título, los indicadores y la tabla (limitada en pantalla; el CSV lleva todas las filas). */
    private fun mostrarInforme(informe: Informe) {
        binding.contenedorResultado.isVisible = true
        binding.tvTituloInforme.text = informe.titulo
        binding.tvRangoInforme.text = informe.rango
        binding.contenedorIndicadores.removeAllViews()
        informe.indicadores.forEach { indicador ->
            ItemMetricaBinding.inflate(layoutInflater, binding.contenedorIndicadores, true).apply {
                tvCifra.text = indicador.valor
                tvEtiqueta.text = indicador.etiqueta
            }
        }
        binding.contenedorTabla.removeAllViews()
        binding.tvSinDatos.isVisible = informe.filas.isEmpty()
        if (informe.filas.isNotEmpty()) {
            agregarFila(informe.columnas, encabezado = true, alterna = false)
            informe.filas.take(MAX_FILAS_PANTALLA).forEachIndexed { indice, fila -> agregarFila(fila, false, indice % 2 == 1) }
        }
        val ocultas = informe.filas.size - MAX_FILAS_PANTALLA
        binding.tvFilasOcultas.isVisible = ocultas > 0
        binding.tvFilasOcultas.text = getString(R.string.informes_filas_ocultas, ocultas)
        binding.btnExportar.isEnabled = informe.filas.isNotEmpty()
    }

    /** Agrega una fila a la tabla: encabezado resaltado y filas alternadas para leer mejor. */
    private fun agregarFila(celdas: List<String>, encabezado: Boolean, alterna: Boolean) {
        val fila = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val fondo = when {
            encabezado -> R.color.naranja_suave
            alterna -> R.color.fondo
            else -> R.color.blanco
        }
        fila.setBackgroundColor(ContextCompat.getColor(requireContext(), fondo))
        celdas.forEach { texto ->
            ItemCeldaInformeBinding.inflate(layoutInflater, fila, true).root.apply {
                text = texto
                if (encabezado) setTypeface(typeface, Typeface.BOLD)
            }
        }
        binding.contenedorTabla.addView(fila)
    }

    /** Exporta el último informe a CSV y abre el selector para compartirlo o abrirlo en Excel. */
    private fun exportar() {
        val informe = ultimoInforme ?: return
        val nombre = "MotoMarket_${getString(tipoElegido.titulo).replace(" ", "_")}_${formatearFecha(System.currentTimeMillis()).replace("/", "-")}"
        viewLifecycleOwner.lifecycleScope.launch {
            val intent = requireContext().intentar {
                ExportadorCsv.exportar(requireContext(), nombre, listOf(informe.titulo, informe.rango), informe.columnas, informe.filas)
            } ?: return@launch
            startActivity(Intent.createChooser(intent, getString(R.string.informes_compartir)))
        }
    }

    /** Primer milisegundo local del día elegido en el calendario. */
    private fun inicioDelDia(utc: Long): Long = diaLocal(utc, 0, 0, 0, 0)

    /** Último milisegundo local del día elegido en el calendario. */
    private fun finDelDia(utc: Long): Long = diaLocal(utc, 23, 59, 59, 999)

    /** Toma la fecha (día) de un instante UTC y devuelve esa misma fecha a la hora local indicada. */
    private fun diaLocal(utc: Long, hora: Int, minuto: Int, segundo: Int, milis: Int): Long {
        val enUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        return Calendar.getInstance().apply {
            set(enUtc.get(Calendar.YEAR), enUtc.get(Calendar.MONTH), enUtc.get(Calendar.DAY_OF_MONTH), hora, minuto, segundo)
            set(Calendar.MILLISECOND, milis)
        }.timeInMillis
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ADMIN = "es_admin"
        private const val ARG_BARRA = "mostrar_barra"
        private const val MAX_FILAS_PANTALLA = 100

        /** [esAdmin] habilita los informes de toda la plataforma; [mostrarBarra] es false dentro del menú del admin. */
        fun nuevo(esAdmin: Boolean, mostrarBarra: Boolean): InformesFragment =
            InformesFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_ADMIN, esAdmin)
                    putBoolean(ARG_BARRA, mostrarBarra)
                }
            }
    }
}
