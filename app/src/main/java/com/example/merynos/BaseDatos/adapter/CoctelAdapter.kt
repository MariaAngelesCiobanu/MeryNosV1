package com.example.merynos.BaseDatos.adapter // O el paquete donde realmente lo tengas

import android.view.LayoutInflater
import android.view.View // Importa View para controlar la visibilidad
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemCoctelBinding // Asegúrate que el binding se genera con este nombre

// Data class ItemCoctel MODIFICADA
data class ItemCoctel(
    val id_coctel: Int, // <--- CAMPO AÑADIDO
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagenResId: Int
)

// Clase CoctelAdapter MODIFICADA
class CoctelAdapter(
    private val lista: List<ItemCoctel>,
    private val esAdmin: Boolean,                   // <--- NUEVO PARÁMETRO
    private val onAddClick: (ItemCoctel) -> Unit,   // Para clientes: añadir al pedido
    private val onEditClick: (ItemCoctel) -> Unit,  // Para admin: editar cóctel
    private val onDeleteClick: (ItemCoctel) -> Unit // Para admin: borrar cóctel
) : RecyclerView.Adapter<CoctelAdapter.CoctelViewHolder>() {

    inner class CoctelViewHolder(val binding: ItemCoctelBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoctelViewHolder {
        val binding = ItemCoctelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CoctelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoctelViewHolder, position: Int) {
        val item = lista[position]
        with(holder.binding) {
            txtNombreCoctel.text = item.nombre
            txtDescripcionCoctel.text = item.descripcion
            txtPrecioCoctel.text = "€%.2f".format(item.precio) // Formato de precio
            imgCoctel.setImageResource(item.imagenResId) // Asigna imagen

            if (esAdmin) {
                // Vista para el Administrador
                btnAnadir.visibility = View.GONE // Ocultar botón de añadir a pedido
                layoutAdminControles.visibility = View.VISIBLE // Mostrar controles de admin (editar, borrar)

                btnEditarCoctelAdmin.setOnClickListener {
                    onEditClick(item)
                }
                btnBorrarCoctelAdmin.setOnClickListener {
                    onDeleteClick(item)
                }
            } else {
                // Vista para el Cliente
                btnAnadir.visibility = View.VISIBLE // Mostrar botón de añadir a pedido
                layoutAdminControles.visibility = View.GONE // Ocultar controles de admin

                btnAnadir.setOnClickListener {
                    onAddClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = lista.size
}