package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.BarmanAdapter
import com.example.merynos.databinding.ActivityBarmanBinding
import kotlinx.coroutines.launch

class BarmanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarmanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarmanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        // Cargar pedidos pendientes y mostrarlos
        lifecycleScope.launch {
            val pedidosPendientes = db.pedidoDao().obtenerPorEstado("pendiente")

            runOnUiThread {
                if (pedidosPendientes.isEmpty()) {
                    Toast.makeText(this@BarmanActivity, "No hay pedidos pendientes", Toast.LENGTH_SHORT).show()
                } else {
                    binding.recyclerPedidosBarman.layoutManager = LinearLayoutManager(this@BarmanActivity)
                    binding.recyclerPedidosBarman.adapter = BarmanAdapter(pedidosPendientes) { pedido ->
                        val intent = Intent(this@BarmanActivity, DetallePedidoActivity::class.java)
                        intent.putExtra("pedidoId", pedido.id_pedido)
                        intent.putExtra("mesa", "Mesa ${pedido.id_mesa}")
                        startActivity(intent)
                    }
                }
            }
        }

        // Botón cerrar sesión
        binding.btnCerrarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
