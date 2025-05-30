package com.example.merynos // O tu paquete

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.ItemPedido // Asegúrate de que el import es correcto
import com.example.merynos.BaseDatos.adapter.PedidoAdapter // Asegúrate de que el import es correcto
import com.example.merynos.databinding.ActivityPedidoBinding
import com.example.merynos.room.CoctelEntity // Necesario para el tipo de 'coctel'
import kotlinx.coroutines.launch

class PedidoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPedidoBinding
    private lateinit var db: AppDatabase // db como propiedad de clase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar db (solo una vez en onCreate)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration() // Recomendado para desarrollo
            .build()

        val codigoMesa = intent.getStringExtra("mesa") ?: ""
        if (codigoMesa.isEmpty()) {
            Toast.makeText(this, "Error: Código de mesa no proporcionado.", Toast.LENGTH_LONG).show()
            finish() // Cierra la actividad si no hay código de mesa
            return
        }

        lifecycleScope.launch {
            // --- LLAMADA A MESADAO CORREGIDA (asumiendo que tienes getMesaPorCodigoQR) ---
            val mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesa)
            if (mesa == null) {
                runOnUiThread {
                    Toast.makeText(this@PedidoActivity, "Mesa '$codigoMesa' no encontrada", Toast.LENGTH_SHORT).show()
                    // Considera finalizar la actividad o mostrar un estado vacío claro
                    binding.txtEstadoPedido.text = "Mesa no válida"
                }
                return@launch
            }

            // Asumiendo que PedidoEntity tiene un campo id_mesa
            val pedidos = db.pedidoDao().obtenerPorEstado("pendiente") // Obtiene todos los pendientes
            val pedidoMesa = pedidos.find { it.id_mesa == mesa.id_mesa } // Filtra por la mesa actual

            if (pedidoMesa != null) {
                val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoMesa.id_pedido)

                // --- BLOQUE .map CORREGIDO PARA MANEJAR COCTEL NULABLE ---
                val listaItems = detalles.map { detalle ->
                    // db.coctelDao().getCoctelPorId() ahora debería devolver CoctelEntity?
                    val coctel: CoctelEntity? = db.coctelDao().getCoctelPorId(detalle.id_coctel)

                    ItemPedido(
                        nombre = coctel?.nombreCoctel ?: "Cóctel no disponible", // Llamada segura y valor por defecto
                        cantidad = detalle.cantidad,
                        precioUnitario = coctel?.precioCoctel ?: 0.0 // Llamada segura y valor por defecto
                    )
                }
                // --- FIN BLOQUE .map CORREGIDO ---

                runOnUiThread {
                    binding.txtMesaPedido.text = "Mesa: ${mesa.nombreMesa}" // Muestra el nombre de la mesa
                    binding.txtEstadoPedido.text = "Estado: ${pedidoMesa.estado}"
                    if(listaItems.isEmpty()){
                        binding.txtTotalPedido.text = "Total: €0.00 - Pedido vacío"
                    } else {
                        val totalPedido = listaItems.sumOf { it.cantidad * it.precioUnitario }
                        binding.txtTotalPedido.text = "Total: €%.2f".format(totalPedido)
                    }
                    binding.recyclerPedido.layoutManager = LinearLayoutManager(this@PedidoActivity)
                    // Asegúrate de que PedidoAdapter espera List<ItemPedido>
                    binding.recyclerPedido.adapter = PedidoAdapter(listaItems)
                }
            } else {
                runOnUiThread {
                    binding.txtMesaPedido.text = "Mesa: ${mesa.nombreMesa}"
                    binding.txtEstadoPedido.text = "No hay pedidos pendientes para esta mesa"
                    binding.txtTotalPedido.text = "Total: €0.00"
                    // Puedes pasar una lista vacía al adaptador si quieres que el RecyclerView se muestre vacío
                    binding.recyclerPedido.layoutManager = LinearLayoutManager(this@PedidoActivity)
                    binding.recyclerPedido.adapter = PedidoAdapter(emptyList())
                }
            }
        }

        binding.btnPedirCuenta.setOnClickListener {
            // Aquí necesitarás el id_pedido del pedidoMesa para marcarlo como 'solicitado' o 'pagado'
            lifecycleScope.launch {
                val mesaEntity = db.mesaDao().getMesaPorCodigoQR(codigoMesa)
                if(mesaEntity != null){
                    val pedidosPendientes = db.pedidoDao().obtenerPorEstado("pendiente")
                    val pedidoActual = pedidosPendientes.find { it.id_mesa == mesaEntity.id_mesa }
                    if (pedidoActual != null) {
                        db.pedidoDao().actualizarEstado(pedidoActual.id_pedido, "pagado") // o "cuenta_solicitada"
                        runOnUiThread {
                            Toast.makeText(this@PedidoActivity, "Cuenta solicitada / Pedido cerrado", Toast.LENGTH_LONG).show()
                            binding.txtEstadoPedido.text = "Estado: pagado" // Actualiza UI
                            // Podrías querer deshabilitar el botón o finalizar la actividad
                            // finish() o navegar a otra pantalla
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@PedidoActivity, "No hay pedido activo para cerrar.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}