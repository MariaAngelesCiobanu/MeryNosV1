package com.example.merynos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.merynos.databinding.ActivityCarritoBinding
import com.example.merynos.BaseDatos.adapter.CarritoAdapter
import com.example.merynos.BaseDatos.adapter.ItemCarrito

class CarritoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarritoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simulación: lista vacía (el adaptador se añadirá después)
        // binding.recyclerCarrito.adapter = CarritoAdapter(listaCoctelesSeleccionados)

        binding.recyclerCarrito.layoutManager = LinearLayoutManager(this)

        binding.txtTotalCarrito.text = "Total: 0.00€"

        binding.btnConfirmarPedido.setOnClickListener {
            Toast.makeText(this, "Confirmación de pedido (lógica pendiente)", Toast.LENGTH_SHORT).show()
        }
    }
}
