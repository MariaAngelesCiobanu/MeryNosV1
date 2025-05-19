package com.example.merynos.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemCarritoBinding

data class ItemCarrito(
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double
)

class CarritoAdapter(
    private val lista: List<ItemCarrito>
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    inner class CarritoViewHolder(val binding: ItemCarritoBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = lista[position]
        with(holder.binding) {
            txtNombreCoctel.text = item.nombre
            txtCantidad.text = "x${item.cantidad}"
            txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad)
        }
    }

    override fun getItemCount(): Int = lista.size
}