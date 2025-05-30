
package com.example.merynos.BaseDatos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemHistorialBinding
import com.example.merynos.room.PedidoEntity

class HistorialAdapter(
    private val lista: List<PedidoEntity>
) : RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    inner class HistorialViewHolder(val binding: ItemHistorialBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val binding = ItemHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistorialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val pedido = lista[position]
        with(holder.binding) {
            txtPedidoHistorial.text = "Pedido #${pedido.id_pedido} - Estado: ${pedido.estado} - Mesa ${pedido.id_mesa}"
        }
    }

    override fun getItemCount(): Int = lista.size
}
