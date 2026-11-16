package com.motomarket.app.ui.vendedor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.motomarket.app.R
import com.motomarket.app.databinding.ItemPedidoBinding
import com.motomarket.app.models.Pedido
import com.motomarket.app.ui.common.mostrarEstado
import com.motomarket.app.util.formatearCOP
import com.motomarket.app.util.formatearFecha

/**
 * Lista de órdenes compartida por el historial del comprador y los pedidos del vendedor.
 * Con [vendedorId] solo muestra los productos y el valor que corresponden a ese vendedor.
 */
class PedidoAdapter(
    private val vendedorId: String? = null,
    private val onPedidoClick: ((Pedido) -> Unit)? = null
) : ListAdapter<Pedido, PedidoAdapter.PedidoViewHolder>(PedidoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) = holder.bind(getItem(position))

    /** Número, fecha, productos, valor y chip de estado de la orden. */
    inner class PedidoViewHolder(private val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pedido: Pedido) {
            val context = binding.root.context
            val items = vendedorId?.let { pedido.itemsDeVendedor(it) } ?: pedido.items
            binding.tvNumero.text = context.getString(R.string.pedidos_numero, pedido.numero)
            binding.tvProducto.text = context.getString(
                R.string.pedidos_detalle,
                formatearFecha(pedido.fecha),
                items.joinToString { context.getString(R.string.pedidos_item, it.nombre, it.cantidad) }
            )
            binding.tvValor.text = formatearCOP(if (vendedorId != null) items.sumOf { it.subtotal } else pedido.total)
            binding.tvEstado.mostrarEstado(pedido.estado)
            binding.root.setOnClickListener { onPedidoClick?.invoke(pedido) }
        }
    }
}

/** Compara por id y por contenido para que la lista solo redibuje los elementos que cambian. */
private object PedidoDiff : DiffUtil.ItemCallback<Pedido>() {
    override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
}
