
package com.example.merynos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.merynos.databinding.ItemCoctelBinding

data class ItemCoctel(
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagenResId: Int
)

class CoctelAdapter(
    private val lista: List<ItemCoctel>,
    private val onAddClick: (ItemCoctel) -> Unit
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
            txtPrecioCoctel.text = "€%.2f".format(item.precio)
            imgCoctel.setImageResource(item.imagenResId)

            btnAnadir.setOnClickListener {
                onAddClick(item)
            }
        }
    }

    override fun getItemCount(): Int = lista.size
}
