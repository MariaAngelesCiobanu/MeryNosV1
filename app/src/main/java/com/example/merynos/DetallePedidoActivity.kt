package com.example.merynos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityDetallePedidoBinding
import com.example.merynos.adapter.DetallePedidoAdapter
import com.example.merynos.adapter.ItemDetallePedido
import kotlinx.coroutines.launch


class DetallePedidoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetallePedidoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetallePedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pedidoId = intent.getIntExtra("pedidoId", -1)
        val mesaNombre = intent.getStringExtra("mesa") ?: "Mesa desconocida"

        binding.txtMesaDetalle.text = "Mesa: $mesaNombre"

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        lifecycleScope.launch {
            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoId)

            val listaItems = detalles.map { detalle ->
                val coctel = db.coctelDao().getCoctelPorId(detalle.id_coctel)
                ItemDetallePedido(
                    nombre = coctel.nombreCoctel,
                    cantidad = detalle.cantidad,
                    precio = coctel.precioCoctel
                )
            }

            runOnUiThread {
                binding.recyclerDetallePedido.layoutManager = LinearLayoutManager(this@DetallePedidoActivity)
                binding.recyclerDetallePedido.adapter = DetallePedidoAdapter(listaItems)
            }
        }

        binding.btnMarcarPreparado.setOnClickListener {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "merynos.db"
            ).build()

            lifecycleScope.launch {
                db.pedidoDao().actualizarEstado(pedidoId, "preparado")
                runOnUiThread {
                    Toast.makeText(this@DetallePedidoActivity, "Pedido marcado como preparado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
