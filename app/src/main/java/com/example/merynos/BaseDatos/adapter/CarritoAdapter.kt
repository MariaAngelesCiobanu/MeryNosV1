package com.example.merynos.BaseDatos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemCarritoBinding

data class ItemCarrito(
    val id_detalle_pedido: Int? = null,
    val id_coctel: Int,
    val nombre: String,
    var cantidad: Int,
    val precioUnitario: Double,
    val imagenResId: Int? = null
)

class CarritoAdapter(
    private var lista: MutableList<ItemCarrito>,
    private val onCantidadChangeListener: OnCantidadChangeListener
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    interface OnCantidadChangeListener {
        fun onCantidadChanged(itemCarrito: ItemCarrito)
    }

    inner class CarritoViewHolder(val binding: ItemCarritoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnAumentarCantidad.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = lista[position]
                    item.cantidad++
                    binding.txtCantidad.text = "x${item.cantidad}"
                    binding.txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad)
                    onCantidadChangeListener.onCantidadChanged(item)
                }
            }

            binding.btnDisminuirCantidad.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = lista[position]
                    if (item.cantidad > 1) {
                        item.cantidad--
                        binding.txtCantidad.text = "x${item.cantidad}"
                        binding.txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad)
                        onCantidadChangeListener.onCantidadChanged(item)
                    } else if (item.cantidad == 1) {
                        // Opcional: Aquí puedes implementar la lógica para eliminar el item del carrito
                        // Por ahora, solo notifica el cambio (la cantidad sigue siendo 1)
                        onCantidadChangeListener.onCantidadChanged(item)
                    }
                }
            }
        }

        fun bind(item: ItemCarrito) {
            binding.txtNombreCoctel.text = item.nombre
            binding.txtCantidad.text = "x${item.cantidad}"
            binding.txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad)
            // Si necesitas mostrar la imagen aquí, lo harías con item.imagenResId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<ItemCarrito>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}