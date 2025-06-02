package com.example.merynos.BaseDatos.adapter // O el paquete donde tengas este archivo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemCarritoBinding // Asegúrate que tu layout se llama item_carrito.xml


data class ItemCarrito(
    val id_detalle_pedido: Int,
    val id_coctel: Int,
    val nombre: String,
    var cantidad: Int,
    val precioUnitario: Double,
    val imagenResId: Int? = null
)

class CarritoAdapter(
    private var lista: List<ItemCarrito> // Lista de los ítems a mostrar
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    inner class CarritoViewHolder(val binding: ItemCarritoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        // Inflar el layout para cada ítem del carrito
        // Asegúrate que tu archivo XML se llama item_carrito.xml
        val binding = ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarritoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = lista[position] // Obtener el ítem actual de la lista
        with(holder.binding) {
            // Asignar los datos del 'item' a las vistas definidas en item_carrito.xml
            // Usamos los IDs que me pasaste en tu último item_carrito.xml:
            // txtNombreCoctel, txtCantidad, txtSubtotal

            txtNombreCoctel.text = item.nombre
            txtCantidad.text = "x${item.cantidad}" // Formato para mostrar "x1", "x2", etc.
            txtSubtotal.text = "€%.2f".format(item.precioUnitario * item.cantidad) // Calcular y formatear subtotal

            // Si tuvieras botones de acción en item_carrito.xml (como quitar, +/- cantidad),
            // aquí configurarías sus OnClickListeners.
        }
    }

    override fun getItemCount(): Int = lista.size

    // Función para que CarritoActivity pueda actualizar la lista del adaptador
    fun actualizarLista(nuevaLista: List<ItemCarrito>) {
        lista = nuevaLista
        notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado
        // Para listas grandes, considera usar DiffUtil para mejor rendimiento.
    }
}