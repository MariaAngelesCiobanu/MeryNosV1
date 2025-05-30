package com.example.merynos // O tu paquete

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityDetallePedidoBinding
import com.example.merynos.BaseDatos.adapter.DetallePedidoAdapter // Asegúrate que el import es correcto
import com.example.merynos.BaseDatos.adapter.ItemDetallePedido    // Asegúrate que el import es correcto
import com.example.merynos.room.CoctelEntity // Necesario para el tipo de 'coctel'
import kotlinx.coroutines.launch


class DetallePedidoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetallePedidoBinding
    private lateinit var db: AppDatabase // db como propiedad de la clase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetallePedidoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar db (solo una vez en onCreate)
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration() // Recomendado para desarrollo
            .build()

        val pedidoId = intent.getIntExtra("pedidoId", -1)
        if (pedidoId == -1) {
            Toast.makeText(this, "Error: ID de pedido no válido", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val mesaNombre = intent.getStringExtra("mesa") ?: "Mesa desconocida"

        binding.txtMesaDetalle.text = "Mesa: $mesaNombre"


        lifecycleScope.launch {
            // Asumo que obtenerDetallesPedido devuelve List<DetallePedidoEntity>
            // y que DetallePedidoEntity tiene un campo id_coctel: Int (no nulable)
            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoId)

            // --- BLOQUE .map CORREGIDO ---
            val listaItems = detalles.map { detalle ->
                // db.coctelDao().getCoctelPorId() ahora debería devolver CoctelEntity?
                val coctel: CoctelEntity? = db.coctelDao().getCoctelPorId(detalle.id_coctel)

                ItemDetallePedido(
                    nombre = coctel?.nombreCoctel ?: "Cóctel Desconocido", // Llamada segura y valor por defecto
                    cantidad = detalle.cantidad,
                    precio = coctel?.precioCoctel ?: 0.0 // Llamada segura y valor por defecto
                )
            }
            // --- FIN BLOQUE .map CORREGIDO ---

            runOnUiThread {
                if (listaItems.isEmpty()){
                    Toast.makeText(this@DetallePedidoActivity, "Este pedido no tiene detalles.", Toast.LENGTH_SHORT).show()
                }
                binding.recyclerDetallePedido.layoutManager = LinearLayoutManager(this@DetallePedidoActivity)
                // Asegúrate de que DetallePedidoAdapter espera List<ItemDetallePedido>
                binding.recyclerDetallePedido.adapter = DetallePedidoAdapter(listaItems)
            }
        }

        binding.btnMarcarPreparado.setOnClickListener {
            // --- OPTIMIZACIÓN: Usar la instancia 'db' de la clase ---
            // Ya no es necesario: val db = Room.databaseBuilder(...)

            lifecycleScope.launch {
                db.pedidoDao().actualizarEstado(pedidoId, "preparado")
                runOnUiThread {
                    Toast.makeText(this@DetallePedidoActivity, "Pedido marcado como preparado", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // Para notificar a la actividad anterior si es necesario
                    finish()
                }
            }
        }
    }
}