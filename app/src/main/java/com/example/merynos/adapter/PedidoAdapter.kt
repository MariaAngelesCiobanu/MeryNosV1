
package com.example.merynos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemPedidoBinding

data class ItemPedido(
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double
)

class PedidoAdapter(
    private val lista: List<ItemPedido>
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(val binding: ItemPedidoBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val item = lista[position]
        with(holder.binding) {
            txtNombrePedido.text = item.nombre
            txtCantidadPedido.text = "x${item.cantidad}"
            txtSubtotalPedido.text = "€%.2f".format(item.cantidad * item.precioUnitario)
        }
    }

    override fun getItemCount(): Int = lista.size
}
