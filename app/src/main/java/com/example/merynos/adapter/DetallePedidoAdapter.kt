
package com.example.merynos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemDetallePedidoBinding

data class ItemDetallePedido(
    val nombre: String,
    val cantidad: Int,
    val precio: Double
)

class DetallePedidoAdapter(
    private val lista: List<ItemDetallePedido>
) : RecyclerView.Adapter<DetallePedidoAdapter.DetalleViewHolder>() {

    inner class DetalleViewHolder(val binding: ItemDetallePedidoBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val binding = ItemDetallePedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetalleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        val item = lista[position]
        with(holder.binding) {
            txtNombreDetalle.text = item.nombre
            txtCantidadDetalle.text = "x${item.cantidad}"
            txtPrecioDetalle.text = "€%.2f".format(item.cantidad * item.precio)
        }
    }

    override fun getItemCount(): Int = lista.size
}
