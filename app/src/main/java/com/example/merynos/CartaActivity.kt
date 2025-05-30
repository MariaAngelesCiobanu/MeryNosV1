package com.example.merynos // O tu paquete

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Importa View para controlar la visibilidad
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Para el resultado de AddEditCoctelActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.CoctelAdapter // Asegúrate que esta es la ruta correcta
import com.example.merynos.BaseDatos.adapter.ItemCoctel    // Asegúrate que esta es la ruta correcta
import com.example.merynos.databinding.ActivityCartaBinding
import com.example.merynos.room.DetallePedidoEntity
import com.example.merynos.room.PedidoEntity
import kotlinx.coroutines.launch

class CartaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartaBinding
    private lateinit var db: AppDatabase
    private var idMesaGlobal: Int? = null
    private var idUsuarioActual: Int? = null
    private var esAdmin: Boolean = false

    private val addEditCoctelActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigoMesaActual = binding.txtMesa.text.toString().substringAfter("📍").trim()
                if (codigoMesaActual.isNotEmpty() && codigoMesaActual != "Mesa desconocida" && codigoMesaActual != "Administración") {
                    cargarDatosDeLaCarta(codigoMesaActual)
                } else if (esAdmin) {
                    cargarDatosDeLaCarta(null) // Admin refresca la lista general
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val rolUsuario = intent.getStringExtra("rol")
        // El valor por defecto 0 para idUsuarioActual podría necesitar revisión si 0 es un ID válido.
        // Usualmente, los IDs de base de datos autogenerados empiezan en 1.
        idUsuarioActual = intent.getIntExtra("usuario_id", 0)
        esAdmin = (rolUsuario == "admin")

        val codigoMesaIntent = intent.getStringExtra("mesa")

        if (esAdmin && codigoMesaIntent == null) {
            // Admin entra directamente a la carta general, no a una mesa específica
            binding.txtMesa.text = "📍 Administración de Cócteles"
            cargarDatosDeLaCarta(null) // El admin ve todos los cócteles
        } else if (codigoMesaIntent != null) {
            binding.txtMesa.text = "📍 $codigoMesaIntent"
            cargarDatosDeLaCarta(codigoMesaIntent)
        } else {
            // Caso no esperado: no es admin y no hay código de mesa. Podría mostrar diálogo o finalizar.
            Toast.makeText(this, "Error: Mesa no especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (esAdmin) {
            binding.btnAnadirCoctelAdmin.visibility = View.VISIBLE
            binding.btnAnadirCoctelAdmin.setOnClickListener {
                val intent = Intent(this, AddEditCoctelActivity::class.java) // Aún dará error hasta crear AddEditCoctelActivity
                addEditCoctelActivityResultLauncher.launch(intent)
            }
        } else {
            binding.btnAnadirCoctelAdmin.visibility = View.GONE
        }

        binding.btnVolverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun cargarDatosDeLaCarta(codigoMesaQR: String?) {
        lifecycleScope.launch {
            if (codigoMesaQR != null) {
                val mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaQR)
                if (mesa == null) {
                    runOnUiThread {
                        Toast.makeText(this@CartaActivity, "Mesa '$codigoMesaQR' no encontrada", Toast.LENGTH_SHORT).show()
                    }
                    if (!esAdmin) { // Si no es admin y la mesa no se encuentra, no cargar nada más
                        binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), esAdmin, {}, {}, {}) // Adaptador vacío
                        return@launch
                    }
                    // Si es admin, podría querer ver los cócteles igual, así que idMesaGlobal puede quedar null
                }
                idMesaGlobal = mesa?.id_mesa
            } else if (!esAdmin) {
                // No es admin y no hay código de mesa (caso que ya manejamos en onCreate, pero por si acaso)
                Log.e("CartaActivity", "Error: Cliente sin código de mesa intentando cargar carta.")
                runOnUiThread { Toast.makeText(this@CartaActivity, "Error: Mesa no especificada.", Toast.LENGTH_LONG).show() }
                binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), esAdmin, {}, {}, {}) // Adaptador vacío
                return@launch
            }


            val cocteles = db.coctelDao().getAll()
            val listaItems = cocteles.map { coctelEntity ->
                ItemCoctel(
                    id_coctel = coctelEntity.id_coctel,
                    nombre = coctelEntity.nombreCoctel ?: "N/D",
                    descripcion = coctelEntity.metodoElaboracion ?: "N/D",
                    precio = coctelEntity.precioCoctel,
                    imagenResId = R.drawable.placeholder_coctel
                )
            }

            runOnUiThread {
                if (listaItems.isEmpty() && esAdmin) { // Solo mostrar este toast si es admin y no hay cócteles, para que pueda añadir.
                    Toast.makeText(this@CartaActivity, "No hay cócteles. ¡Añade el primero!", Toast.LENGTH_LONG).show()
                } else if (listaItems.isEmpty() && !esAdmin){
                    Toast.makeText(this@CartaActivity, "No hay cócteles disponibles en este momento.", Toast.LENGTH_LONG).show()
                }


                binding.recyclerCocteles.layoutManager = LinearLayoutManager(this@CartaActivity)
                binding.recyclerCocteles.adapter = CoctelAdapter(
                    listaItems,
                    esAdmin,
                    onAddClick = { coctelSeleccionado ->
                        if (esAdmin) {
                            Toast.makeText(this@CartaActivity, "Admin no puede añadir al pedido desde aquí.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idMesaGlobal == null) {
                            Toast.makeText(this@CartaActivity, "Mesa no válida para hacer pedido.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idUsuarioActual == null || idUsuarioActual == 0) { // Asumiendo que 0 no es un ID de usuario válido
                            Toast.makeText(this@CartaActivity, "Usuario no identificado para el pedido.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }

                        lifecycleScope.launch {
                            val coctelEntity = db.coctelDao().getCoctelPorId(coctelSeleccionado.id_coctel)
                            if (coctelEntity == null) {
                                runOnUiThread{ Toast.makeText(this@CartaActivity, "Error: Cóctel no disponible.", Toast.LENGTH_SHORT).show() }
                                return@launch
                            }

                            val pedidoExistente = db.pedidoDao()
                                .obtenerPorMesaYEstado(idMesaGlobal!!, "pendiente")
                                .firstOrNull()

                            val pedidoId: Int
                            if (pedidoExistente != null) {
                                pedidoId = pedidoExistente.id_pedido
                            } else {
                                pedidoId = db.pedidoDao()
                                    .insertarPedido(PedidoEntity(
                                        id_mesa = idMesaGlobal!!,
                                        id_usuario = idUsuarioActual!!
                                    )).toInt()
                            }

                            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoId)
                            val detalleExistente = detalles.find { it.id_coctel == coctelSeleccionado.id_coctel }

                            // --- BLOQUE CORREGIDO ---
                            if (detalleExistente != null) {
                                db.pedidoDao().actualizarCantidad(
                                    pedidoId = pedidoId,
                                    coctelId = detalleExistente.id_coctel, // id_coctel existe en tu DetallePedidoEntity
                                    nuevaCantidad = detalleExistente.cantidad + 1
                                )
                            } else {
                                db.pedidoDao().insertarDetalle(
                                    DetallePedidoEntity(
                                        id_pedido = pedidoId,
                                        id_coctel = coctelSeleccionado.id_coctel, // Asumiendo que ItemCoctel tiene id_coctel
                                        cantidad = 1
                                    )
                                )
                            }
                            // --- FIN BLOQUE CORREGIDO ---

                            runOnUiThread {
                                Toast.makeText(this@CartaActivity, "${coctelSeleccionado.nombre} añadido al pedido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEditClick = { coctelAEditar ->
                        if (esAdmin) {
                            val intent = Intent(this@CartaActivity, AddEditCoctelActivity::class.java) // Aún dará error hasta crear AddEditCoctelActivity
                            intent.putExtra("ID_COCTEL_A_EDITAR", coctelAEditar.id_coctel)
                            addEditCoctelActivityResultLauncher.launch(intent)
                        }
                    },
                    onDeleteClick = { coctelABorrar ->
                        if (esAdmin) {
                            mostrarDialogoConfirmarBorrado(coctelABorrar)
                        }
                    }
                )
            }
        }
    }

    private fun mostrarDialogoConfirmarBorrado(coctel: ItemCoctel) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Estás seguro de que quieres borrar '${coctel.nombre}'?")
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch {
                    val coctelEntity = db.coctelDao().getCoctelPorId(coctel.id_coctel)
                    if (coctelEntity != null) {
                        db.coctelDao().deleteCoctel(coctelEntity)
                        Toast.makeText(this@CartaActivity, "'${coctel.nombre}' borrado", Toast.LENGTH_SHORT).show()

                        val codigoMesaActual = binding.txtMesa.text.toString().substringAfter("📍").trim()
                        if (codigoMesaActual.isNotEmpty() && codigoMesaActual != "Administración de Cócteles" && codigoMesaActual != "Mesa desconocida") {
                            cargarDatosDeLaCarta(codigoMesaActual)
                        } else if (esAdmin) {
                            cargarDatosDeLaCarta(null)
                        }
                    } else {
                        Toast.makeText(this@CartaActivity, "Error: Cóctel no encontrado para borrar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}