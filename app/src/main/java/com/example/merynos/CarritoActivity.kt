package com.example.merynos // Your package

import android.app.Activity
// import android.content.Intent // Not strictly needed if only finishing
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // For confirm order dialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.CarritoAdapter // Your adapter
import com.example.merynos.BaseDatos.adapter.ItemCarrito    // Your data class
import com.example.merynos.R
import com.example.merynos.databinding.ActivityCarritoBinding
import com.example.merynos.room.CoctelEntity
// DetallePedidoEntity is used to fetch, but not directly in ItemCarrito for this version
import com.example.merynos.room.PedidoEntity
import kotlinx.coroutines.launch
import java.util.Locale

class CarritoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarritoBinding
    private lateinit var db: AppDatabase
    private var pedidoIdActual: Int? = null
    private var idMesaCarrito: Int? = null
    private var idUsuarioCarrito: Int? = null
    private lateinit var nombreMesaCarrito: String

    private lateinit var carritoAdapter: CarritoAdapter
    // listaItemsCarrito will now be List<ItemCarrito> based on YOUR ItemCarrito definition
    private val listaItemsCarrito = mutableListOf<ItemCarrito>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarritoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Get data from Intent
        pedidoIdActual = if (intent.hasExtra("ID_PEDIDO_ACTUAL")) intent.getIntExtra("ID_PEDIDO_ACTUAL", -1) else null
        if (pedidoIdActual == -1) pedidoIdActual = null

        idMesaCarrito = if (intent.hasExtra("ID_MESA_CARRITO")) intent.getIntExtra("ID_MESA_CARRITO", -1) else null
        if (idMesaCarrito == -1) idMesaCarrito = null

        idUsuarioCarrito = if (intent.hasExtra("ID_USUARIO_CARRITO")) intent.getIntExtra("ID_USUARIO_CARRITO", -1) else null
        if (idUsuarioCarrito == -1 && idUsuarioCarrito !=0) idUsuarioCarrito = null

        nombreMesaCarrito = intent.getStringExtra("NOMBRE_MESA_CARRITO") ?: "Mesa Desconocida"

        setupRecyclerView()
        cargarDatosDelCarrito()

        binding.txtTituloCarrito?.text = "Carrito - $nombreMesaCarrito"
        binding.txtMesaCarrito?.text = "Mesa: $nombreMesaCarrito"
        binding.txtEstadoActualPedido?.text = "Cargando estado..."
        binding.txtTotalCarrito.text = "Total: €0.00"

        binding.btnConfirmarPedido.setOnClickListener {
            if (pedidoIdActual != null && listaItemsCarrito.isNotEmpty()) {
                confirmarPedido()
            } else {
                Toast.makeText(this, "El carrito está vacío o no hay un pedido activo.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSeguirComprando?.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Instantiate CarritoAdapter with only the list, as per your adapter's constructor
        carritoAdapter = CarritoAdapter(listaItemsCarrito)
        binding.recyclerCarrito.layoutManager = LinearLayoutManager(this)
        binding.recyclerCarrito.adapter = carritoAdapter
    }

    private fun cargarDatosDelCarrito() {
        listaItemsCarrito.clear()

        lifecycleScope.launch {
            var pedidoParaMostrar: PedidoEntity? = null

            if (pedidoIdActual != null) {
                pedidoParaMostrar = db.pedidoDao().obtenerPedidoPorId(pedidoIdActual!!)
            } else if (idMesaCarrito != null && idUsuarioCarrito != null) {
                val idUsuarioNonNull = idUsuarioCarrito ?: 0
                pedidoParaMostrar = db.pedidoDao()
                    .obtenerPorMesaYEstado(idMesaCarrito!!, "pendiente")
                    .find { it.id_usuario == idUsuarioNonNull }
                pedidoIdActual = pedidoParaMostrar?.id_pedido
            }

            if (pedidoParaMostrar == null || pedidoParaMostrar.estado != "pendiente") {
                runOnUiThread {
                    binding.txtTotalCarrito.text = "Total: €0.00"
                    binding.txtEstadoActualPedido?.text = if (pedidoParaMostrar != null) "Pedido ${pedidoParaMostrar.estado}" else "Carrito vacío"
                    // Update adapter with an empty list
                    val currentList = mutableListOf<ItemCarrito>()
                    if(::carritoAdapter.isInitialized) { // Check if adapter is initialized
                        carritoAdapter.actualizarLista(currentList) // You'll need an 'actualizarLista' method in CarritoAdapter
                    } else {
                        // If adapter not init, setupRecyclerView might be called later or handle here
                        listaItemsCarrito.clear()
                        listaItemsCarrito.addAll(currentList)
                        // setupRecyclerView will use this empty list
                    }
                }
                return@launch
            }

            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoParaMostrar.id_pedido)
            var subtotalCalculadoGeneral = 0.0
            val nuevosItemsCarrito = mutableListOf<ItemCarrito>()

            for (detalle in detalles) { // 'detalle' es de tipo DetallePedidoEntity
                // Asumiendo que CoctelDao.getCoctelPorId devuelve CoctelEntity?
                val coctel: CoctelEntity? = db.coctelDao().getCoctelPorId(detalle.id_coctel)

                if (coctel != null) {
                    // --- CREACIÓN DE ItemCarrito CORREGIDA ---
                    nuevosItemsCarrito.add(
                        ItemCarrito(
                            id_detalle_pedido = detalle.id_detalle_pedido, // 'detalle' (DetallePedidoEntity) debe tener este campo
                            id_coctel = detalle.id_coctel,             // 'detalle' (DetallePedidoEntity) tiene este campo. También podrías usar coctel.id_coctel.
                            nombre = coctel.nombreCoctel ?: "Cóctel Desconocido",
                            cantidad = detalle.cantidad,
                            precioUnitario = coctel.precioCoctel ?: 0.0,
                            imagenResId = determinarImagenCoctel(coctel.nombreCoctel) // Tu función auxiliar
                        )
                    )
                    // --- FIN DE LA CORRECCIÓN ---
                    subtotalCalculadoGeneral += (detalle.cantidad * (coctel.precioCoctel ?: 0.0))
                } else {
                    Log.w("CarritoActivity", "No se encontró cóctel ID: ${detalle.id_coctel} para el detalle con id_detalle_pedido: ${detalle.id_detalle_pedido}")
                }
            }
            runOnUiThread {
                binding.txtEstadoActualPedido?.text = "Estado del Pedido: ${pedidoParaMostrar.estado}"
                binding.txtTotalCarrito.text = "Total: €%.2f".format(subtotalCalculadoGeneral)
                if(::carritoAdapter.isInitialized) {
                    carritoAdapter.actualizarLista(nuevosItemsCarrito) // You'll need an 'actualizarLista' method in CarritoAdapter
                } else {
                    listaItemsCarrito.clear()
                    listaItemsCarrito.addAll(nuevosItemsCarrito)
                    // Adapter will be set up with this list in setupRecyclerView
                }

                if (nuevosItemsCarrito.isEmpty() && detalles.isNotEmpty()){
                    Toast.makeText(this@CarritoActivity, "Error al cargar algunos detalles de cócteles.", Toast.LENGTH_SHORT).show()
                } else if (nuevosItemsCarrito.isEmpty()){
                    Toast.makeText(this@CarritoActivity, "El carrito está vacío.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // determinarImagenCoctel is not needed if ItemCarrito doesn't store imagenResId
    /*
    private fun determinarImagenCoctel(nombreCoctel: String?): Int {
        return when (nombreCoctel?.lowercase(Locale.getDefault())) {
            // ... your image mappings ...
            else -> R.drawable.placeholder_coctel
        }
    }
    */

    private fun confirmarPedido() {
        if (pedidoIdActual == null) {
            Toast.makeText(this, "No hay pedido activo para confirmar.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Confirmar Pedido")
            .setMessage("¿Estás seguro de que quieres enviar este pedido a preparación?")
            .setPositiveButton("Confirmar") { dialog, _ ->
                lifecycleScope.launch {
                    db.pedidoDao().actualizarEstado(pedidoIdActual!!, "confirmado")
                    runOnUiThread {
                        Toast.makeText(this@CarritoActivity, "Pedido confirmado y enviado", Toast.LENGTH_LONG).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show();
    }
    private fun determinarImagenCoctel(nombreCoctel: String?): Int {
        return when (nombreCoctel?.lowercase(Locale.getDefault())) { // Usar Locale.getDefault() para consistencia
            "mojito" -> R.drawable.mojito_img          // Asegúrate de tener estos drawables
            "margarita" -> R.drawable.margarita_img
            "cosmopolitan" -> R.drawable.cosmopolitan_img
            "paloma" -> R.drawable.paloma_img
            "negroni" -> R.drawable.negroni_img
            "piña colada" -> R.drawable.pinia_colada_img // Si el nombre en BD es "Piña Colada"
            // "dry martini" -> R.drawable.dry_martini_img // Si tienes imagen para Dry Martini
            // Añade más casos para otros cócteles
            else -> R.drawable.placeholder_coctel     // Imagen por defecto
        }
    }
}