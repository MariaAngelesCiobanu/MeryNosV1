package com.example.merynos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.adapter.HistorialAdapter
import com.example.merynos.databinding.ActivityHistorialPedidosBinding
import kotlinx.coroutines.launch

class HistorialPedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialPedidosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idUsuario = intent.getIntExtra("idUsuario", -1)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        lifecycleScope.launch {
            val historial = db.pedidoDao().obtenerHistorialPorUsuario(idUsuario)

            runOnUiThread {
                binding.recyclerHistorial.layoutManager = LinearLayoutManager(this@HistorialPedidosActivity)
                binding.recyclerHistorial.adapter = HistorialAdapter(historial)
            }
        }

        binding.btnVolverDesdeHistorial.setOnClickListener {
            startActivity(Intent(this, CartaActivity::class.java))
            finish()
        }
    }
}
