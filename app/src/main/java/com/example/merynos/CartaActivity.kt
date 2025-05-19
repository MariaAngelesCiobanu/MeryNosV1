package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.adapter.CoctelAdapter
import com.example.merynos.adapter.ItemCoctel
import com.example.merynos.databinding.ActivityCartaBinding
import com.example.merynos.room.DetallePedidoEntity
import com.example.merynos.room.PedidoEntity
import kotlinx.coroutines.launch

class CartaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartaBinding
    private var idMesa: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val codigoMesa = intent.getStringExtra("mesa") ?: "Mesa desconocida"
        binding.txtMesa.text = "📍 $codigoMesa"

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        lifecycleScope.launch {
            // Buscar mesa en DB
            val mesa = db.mesaDao().getMesaPorCodigo(codigoMesa)
            if (mesa == null) {
                runOnUiThread {
                    Toast.makeText(this@CartaActivity, "Mesa no encontrada", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            idMesa = mesa.id_mesa

            // Cargar cocteles
            val cocteles = db.coctelDao().getAll()
            val listaItems = cocteles.map {
                ItemCoctel(
                    nombre = it.nombreCoctel,
                    descripcion = it.metodoElaboracion,
                    precio = it.precioCoctel,
                    imagenResId = R.drawable.placeholder_coctel
                )
            }

            runOnUiThread {
                binding.recyclerCocteles.layoutManager = LinearLayoutManager(this@CartaActivity)
                binding.recyclerCocteles.adapter = CoctelAdapter(listaItems) { coctelSeleccionado ->
                    // Lógica de añadir al pedido
                    lifecycleScope.launch {
                        val coctelEntity = db.coctelDao().getPorNombre(coctelSeleccionado.nombre)

                        val pedidoExistente = db.pedidoDao()
                            .obtenerPorEstado("pendiente")
                            .find { it.id_mesa == idMesa }

                        val pedidoId = pedidoExistente?.id_pedido ?: db.pedidoDao()
                            .insertarPedido(PedidoEntity(id_mesa = idMesa!!)).toInt()

                        val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoId)
                        val detalleExistente = detalles.find { it.id_coctel == coctelEntity.id_coctel }

                        if (detalleExistente != null) {
                            db.pedidoDao().actualizarCantidad(
                                pedidoId,
                                coctelEntity.id_coctel,
                                detalleExistente.cantidad + 1
                            )
                        } else {
                            db.pedidoDao().insertarDetalle(
                                DetallePedidoEntity(
                                    id_pedido = pedidoId,
                                    id_coctel = coctelEntity.id_coctel,
                                    cantidad = 1
                                )
                            )
                        }

                        runOnUiThread {
                            Toast.makeText(
                                this@CartaActivity,
                                "${coctelSeleccionado.nombre} añadido al pedido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        binding.btnVolverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
