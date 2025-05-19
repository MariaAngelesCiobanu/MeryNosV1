package com.example.merynos

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.adapter.ItemPedido
import com.example.merynos.adapter.PedidoAdapter
import com.example.merynos.databinding.ActivityPedidoBinding
import kotlinx.coroutines.launch

class PedidoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPedidoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val codigoMesa = intent.getStringExtra("mesa") ?: ""

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        lifecycleScope.launch {
            val mesa = db.mesaDao().getMesaPorCodigo(codigoMesa)
            if (mesa == null) {
                runOnUiThread {
                    Toast.makeText(this@PedidoActivity, "Mesa no encontrada", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val pedidos = db.pedidoDao().obtenerPorEstado("pendiente")
            val pedidoMesa = pedidos.find { it.id_mesa == mesa.id_mesa }

            if (pedidoMesa != null) {
                val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoMesa.id_pedido)

                val listaItems = detalles.map { detalle ->
                    val coctel = db.coctelDao().getCoctelPorId(detalle.id_coctel)
                    ItemPedido(
                        nombre = coctel.nombreCoctel,
                        cantidad = detalle.cantidad,
                        precioUnitario = coctel.precioCoctel
                    )
                }

                runOnUiThread {
                    binding.txtEstadoPedido.text = "Estado: ${pedidoMesa.estado}"
                    binding.recyclerPedido.layoutManager = LinearLayoutManager(this@PedidoActivity)
                    binding.recyclerPedido.adapter = PedidoAdapter(listaItems)
                }
            } else {
                runOnUiThread {
                    binding.txtEstadoPedido.text = "No hay pedidos pendientes"
                }
            }
        }

        binding.btnPedirCuenta.setOnClickListener {
            Toast.makeText(this, "Solicitaste la cuenta (lógica pendiente)", Toast.LENGTH_SHORT).show()
        }
    }
}
