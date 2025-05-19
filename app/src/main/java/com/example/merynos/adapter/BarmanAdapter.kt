
package com.example.merynos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemPedidoBarmanBinding
import com.example.merynos.room.PedidoEntity

class BarmanAdapter(
    private val lista: List<PedidoEntity>,
    private val onPedidoClick: (PedidoEntity) -> Unit
) : RecyclerView.Adapter<BarmanAdapter.BarmanViewHolder>() {

    inner class BarmanViewHolder(val binding: ItemPedidoBarmanBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarmanViewHolder {
        val binding = ItemPedidoBarmanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BarmanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarmanViewHolder, position: Int) {
        val pedido = lista[position]
        with(holder.binding) {
            txtPedidoBarman.text = "Pedido #${pedido.id_pedido} - Mesa ${pedido.id_mesa}"
            root.setOnClickListener {
                onPedidoClick(pedido)
            }
        }
    }

    override fun getItemCount(): Int = lista.size
}
