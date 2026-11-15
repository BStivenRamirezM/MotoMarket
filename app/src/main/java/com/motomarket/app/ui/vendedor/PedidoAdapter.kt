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

class PedidoAdapter : ListAdapter<Pedido, PedidoAdapter.PedidoViewHolder>(PedidoDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) = holder.bind(getItem(position))

    class PedidoViewHolder(private val binding: ItemPedidoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pedido: Pedido) {
            binding.tvNumero.text = binding.root.context.getString(R.string.pedidos_numero, pedido.id)
            binding.tvProducto.text = pedido.producto
            binding.tvValor.text = formatearCOP(pedido.valor)
            binding.tvEstado.mostrarEstado(pedido.estado)
        }
    }
}

private object PedidoDiff : DiffUtil.ItemCallback<Pedido>() {
    override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
}
