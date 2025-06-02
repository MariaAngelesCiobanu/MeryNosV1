package com.example.merynos // O tu paquete

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.CoctelAdapter
import com.example.merynos.BaseDatos.adapter.ItemCoctel
import com.example.merynos.R // Asegúrate de importar R para acceder a los drawables
import com.example.merynos.databinding.ActivityCartaBinding
import com.example.merynos.room.DetallePedidoEntity
import com.example.merynos.room.PedidoEntity
import kotlinx.coroutines.launch
import java.util.Locale // Para toLowerCase con Locale

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
                if (codigoMesaActual.isNotEmpty() && codigoMesaActual != "Mesa desconocida" && codigoMesaActual != "Administración de Cócteles") {
                    cargarDatosDeLaCarta(codigoMesaActual)
                } else if (esAdmin) {
                    cargarDatosDeLaCarta(null)
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
        idUsuarioActual = intent.getIntExtra("usuario_id", 0) // 0 podría ser para invitado
        esAdmin = (rolUsuario == "admin")

        val codigoMesaIntent = intent.getStringExtra("mesa")

        if (esAdmin && codigoMesaIntent == null) {
            binding.txtMesa.text = "📍 Administración de Cócteles"
            cargarDatosDeLaCarta(null) // Admin ve todos los cócteles, no asociado a mesa para pedidos
        } else if (codigoMesaIntent != null) {
            binding.txtMesa.text = "📍 $codigoMesaIntent"
            cargarDatosDeLaCarta(codigoMesaIntent)
        } else {
            // No es admin y no hay código de mesa
            Toast.makeText(this, "Error: Mesa no especificada.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Configurar visibilidad y acción del botón "Añadir Cóctel" para Admin
        if (esAdmin) {
            binding.btnAnadirCoctelAdmin.visibility = View.VISIBLE // Asegúrate que este ID existe en XML
            binding.btnAnadirCoctelAdmin.setOnClickListener {
                val intent = Intent(this, AddEditCoctelActivity::class.java) // Necesita AddEditCoctelActivity.kt
                addEditCoctelActivityResultLauncher.launch(intent)
            }
        } else {
            binding.btnAnadirCoctelAdmin.visibility = View.GONE
        }

        // Configurar listener para el Botón Ver Carrito
        binding.btnVerCarrito.setOnClickListener { // Asegúrate que este ID existe en XML
            if (idMesaGlobal != null && idUsuarioActual != null) { // Necesario para asociar el carrito
                // Si es admin en vista general, el carrito de "pedido" no aplica igual
                if (esAdmin && codigoMesaIntent == null) { // Admin en vista general de cócteles
                    Toast.makeText(this, "Función de carrito de cliente no disponible aquí.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val pedidoPendiente = db.pedidoDao()
                        .obtenerPorMesaYEstado(idMesaGlobal!!, "pendiente")
                        .find { it.id_usuario == idUsuarioActual!! }

                    val intent = Intent(this@CartaActivity, CarritoActivity::class.java) // Necesita CarritoActivity.kt
                    if (pedidoPendiente != null) {
                        intent.putExtra("ID_PEDIDO_ACTUAL", pedidoPendiente.id_pedido)
                        Log.d("CartaActivity", "Abriendo carrito para pedido ID: ${pedidoPendiente.id_pedido}")
                    } else {
                        Log.d("CartaActivity", "Abriendo carrito, no hay pedido pendiente. Pasando IDs de mesa/usuario.")
                    }
                    intent.putExtra("ID_MESA_CARRITO", idMesaGlobal)
                    intent.putExtra("ID_USUARIO_CARRITO", idUsuarioActual)
                    intent.putExtra("NOMBRE_MESA_CARRITO", binding.txtMesa.text.toString().substringAfter("📍").trim())
                    startActivity(intent)
                }
            } else if (esAdmin && codigoMesaIntent == null) {
                Toast.makeText(this, "Función de carrito no aplicable en vista de administración general.", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this, "No se puede acceder al carrito: información de mesa o usuario incompleta.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVolverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun cargarDatosDeLaCarta(codigoMesaQR: String?) {
        lifecycleScope.launch {
            if (codigoMesaQR != null) { // Si se especificó una mesa (cliente o admin viendo una mesa específica)
                val mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaQR)
                if (mesa == null) {
                    runOnUiThread {
                        Toast.makeText(this@CartaActivity, "Mesa '$codigoMesaQR' no encontrada", Toast.LENGTH_SHORT).show()
                    }
                    if (!esAdmin) { // Si es cliente y la mesa no existe, no puede continuar
                        binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), false, {}, {}, {})
                        return@launch
                    }
                    // Si es admin y la mesa específica no se encontró, idMesaGlobal quedará null.
                    // En este caso, la lógica de "añadir a pedido" se bloqueará por idMesaGlobal == null si el admin intentara usarla.
                }
                idMesaGlobal = mesa?.id_mesa
            } else if (!esAdmin) { // No hay código de mesa Y NO es admin (es cliente/invitado sin mesa)
                Log.e("CartaActivity_Debug", "Error: Cliente/Invitado sin código de mesa intentando cargar carta.")
                runOnUiThread { Toast.makeText(this@CartaActivity, "Error: Mesa no especificada para el cliente.", Toast.LENGTH_LONG).show() }
                binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), false, {}, {}, {})
                return@launch
            }
            // Si es admin y codigoMesaQR es null (vista de administración general), idMesaGlobal será null.
            // Esto es correcto, ya que el admin en esta vista no hace pedidos, solo gestiona cócteles.

            val cocteles = db.coctelDao().getAll()
            Log.d("CartaActivity_Debug", "Número de CoctelEntity obtenidos de DAO: ${cocteles.size}")

            val listaItems = cocteles.map { coctelEntity ->
                val imagenResIdParaEsteCoctel = when (coctelEntity.nombreCoctel?.lowercase(Locale.getDefault())) {
                    "mojito" -> R.drawable.mojito_img
                    "margarita" -> R.drawable.margarita_img
                    "cosmopolitan" -> R.drawable.cosmopolitan_img
                    "paloma" -> R.drawable.paloma_img
                    "negroni" -> R.drawable.negroni_img
                    "pinia colada" -> R.drawable.pinia_colada_img
                    // "dry martini" -> R.drawable.dry_martini_img // Añade si tienes la imagen
                    else -> R.drawable.placeholder_coctel
                }
                ItemCoctel(
                    id_coctel = coctelEntity.id_coctel, // ItemCoctel debe tener este campo
                    nombre = coctelEntity.nombreCoctel ?: "N/D",
                    descripcion = coctelEntity.metodoElaboracion ?: "N/D",
                    precio = coctelEntity.precioCoctel,
                    imagenResId = imagenResIdParaEsteCoctel
                )
            }
            Log.d("CartaActivity_Debug", "Número de ItemCoctel mapeados (con imágenes específicas): ${listaItems.size}")

            runOnUiThread {
                if (listaItems.isEmpty() && esAdmin) {
                    Toast.makeText(this@CartaActivity, "No hay cócteles. ¡Añade el primero!", Toast.LENGTH_LONG).show()
                } else if (listaItems.isEmpty() && !esAdmin){
                    Toast.makeText(this@CartaActivity, "No hay cócteles disponibles en este momento.", Toast.LENGTH_LONG).show()
                }

                binding.recyclerCocteles.layoutManager = LinearLayoutManager(this@CartaActivity)
                binding.recyclerCocteles.adapter = CoctelAdapter(
                    listaItems,
                    esAdmin,
                    onAddClick = { coctelSeleccionado ->
                        if (esAdmin && idMesaGlobal == null) { // Admin en vista general no puede añadir a pedido
                            Toast.makeText(this@CartaActivity, "Para añadir a un pedido como admin, primero selecciona una mesa.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idMesaGlobal == null) {
                            Toast.makeText(this@CartaActivity, "Mesa no válida para hacer pedido.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idUsuarioActual == null) { // idUsuarioActual = 0 es para invitados
                            Toast.makeText(this@CartaActivity, "Usuario no identificado para el pedido.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }

                        lifecycleScope.launch {
                            val coctelEntity = db.coctelDao().getCoctelPorId(coctelSeleccionado.id_coctel)
                            if (coctelEntity == null) {
                                runOnUiThread{ Toast.makeText(this@CartaActivity, "Error: Cóctel no disponible.", Toast.LENGTH_SHORT).show() }
                                return@launch
                            }

                            val idUsuarioParaPedido = idUsuarioActual!!

                            val pedidoExistente = db.pedidoDao()
                                .obtenerPorMesaYEstado(idMesaGlobal!!, "pendiente")
                                .find { it.id_usuario == idUsuarioParaPedido }

                            val pedidoId: Int
                            if (pedidoExistente != null) {
                                pedidoId = pedidoExistente.id_pedido
                            } else {
                                pedidoId = db.pedidoDao()
                                    .insertarPedido(PedidoEntity(
                                        id_mesa = idMesaGlobal!!,
                                        id_usuario = idUsuarioParaPedido
                                    )).toInt()
                            }

                            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoId)
                            val detalleExistente = detalles.find { it.id_coctel == coctelSeleccionado.id_coctel }

                            if (detalleExistente != null) {
                                db.pedidoDao().actualizarCantidad(
                                    pedidoId = pedidoId,
                                    coctelId = detalleExistente.id_coctel, // DetallePedidoEntity debe tener id_coctel
                                    nuevaCantidad = detalleExistente.cantidad + 1
                                )
                            } else {
                                db.pedidoDao().insertarDetalle(
                                    DetallePedidoEntity(
                                        id_pedido = pedidoId,
                                        id_coctel = coctelSeleccionado.id_coctel, // ItemCoctel debe tener id_coctel
                                        cantidad = 1
                                    )
                                )
                            }
                            runOnUiThread {
                                Toast.makeText(this@CartaActivity, "${coctelSeleccionado.nombre} añadido al pedido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEditClick = { coctelAEditar ->
                        if (esAdmin) {
                            val intent = Intent(this@CartaActivity, AddEditCoctelActivity::class.java)
                            intent.putExtra("ID_COCTEL_A_EDITAR", coctelAEditar.id_coctel) // ItemCoctel debe tener id_coctel
                            addEditCoctelActivityResultLauncher.launch(intent)
                        }
                    },
                    onDeleteClick = { coctelABorrar ->
                        if (esAdmin) {
                            mostrarDialogoConfirmarBorrado(coctelABorrar) // ItemCoctel debe tener id_coctel
                        }
                    }
                )
                Log.d("CartaActivity_Debug", "Adapter configurado. Tamaño de listaItems pasada al adapter: ${listaItems.size}")
            }
        }
    }

    private fun mostrarDialogoConfirmarBorrado(coctel: ItemCoctel) { // ItemCoctel debe tener id_coctel
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Estás seguro de que quieres borrar '${coctel.nombre}'?")
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch {
                    val coctelEntity = db.coctelDao().getCoctelPorId(coctel.id_coctel)
                    if (coctelEntity != null) {
                        db.coctelDao().deleteCoctel(coctelEntity) // CoctelDao debe tener deleteCoctel
                        Toast.makeText(this@CartaActivity, "'${coctel.nombre}' borrado", Toast.LENGTH_SHORT).show()

                        // Refrescar la lista después de borrar
                        val codigoMesaActual = binding.txtMesa.text.toString().substringAfter("📍").trim()
                        if (codigoMesaActual.isNotEmpty() && codigoMesaActual != "Administración de Cócteles" && codigoMesaActual != "Mesa desconocida") {
                            cargarDatosDeLaCarta(codigoMesaActual)
                        } else if (esAdmin) { // Si era la vista de admin general
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