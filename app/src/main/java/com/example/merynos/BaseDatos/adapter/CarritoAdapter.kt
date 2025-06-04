package com.example.merynos.BaseDatos.adapter

import android.view.LayoutInflater
import android.view.View // Importar View para View.VISIBLE/GONE
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
    private val onCantidadChangeListener: OnCantidadChangeListener,
    private val onEliminarCoctelListener: OnEliminarCoctelListener // NUEVO: Listener para eliminar
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    interface OnCantidadChangeListener {
        fun onCantidadChanged(itemCarrito: ItemCarrito)
    }

    interface OnEliminarCoctelListener { // NUEVO: Interfaz para eliminar cóctel
        fun onEliminarCoctel(itemCarrito: ItemCarrito)
    }

    inner class CarritoViewHolder(val binding: ItemCarritoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnAumentarCantidad.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = lista[position]
                    item.cantidad++
                    actualizarVistaItem(item) // Actualizar la vista y visibilidad de papelera
                    onCantidadChangeListener.onCantidadChanged(item)
                }
            }

            binding.btnDisminuirCantidad.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = lista[position]
                    if (item.cantidad > 1) {
                        item.cantidad--
                        actualizarVistaItem(item) // Actualizar la vista y visibilidad de papelera
                        onCantidadChangeListener.onCantidadChanged(item)
                    } else if (item.cantidad == 1) {
                        // Si la cantidad es 1 y se presiona disminuir, no hacemos nada más que ya está en 1
                        // La papelera se encarga de la eliminación completa
                        // onCantidadChangeListener.onCantidadChanged(item) // No es necesario si no cambia la cantidad
                    }
                }
            }

            // NUEVO: Listener para el botón de papelera
            binding.btnEliminarCoctel.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = lista[position]
                    // SOLO si la cantidad es 1, se permite la eliminación completa
                    if (item.cantidad == 1) {
                        onEliminarCoctelListener.onEliminarCoctel(item)
                    }
                }
            }
        }

        fun bind(item: ItemCarrito) {
            binding.txtNombreCoctel.text = item.nombre
            binding.txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad)
            actualizarVistaItem(item) // Llamar para actualizar cantidad y visibilidad de papelera
            // Si necesitas mostrar la imagen aquí, lo harías con item.imagenResId
        }

        private fun actualizarVistaItem(item: ItemCarrito) {
            binding.txtCantidad.text = "x${item.cantidad}"
            // Controlar la visibilidad de la papelera
            if (item.cantidad == 1) {
                binding.btnEliminarCoctel.visibility = View.VISIBLE
                binding.btnDisminuirCantidad.visibility = View.GONE // Ocultar '-' si la cantidad es 1
            } else {
                binding.btnEliminarCoctel.visibility = View.GONE
                binding.btnDisminuirCantidad.visibility = View.VISIBLE // Mostrar '-' si la cantidad es > 1
            }
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