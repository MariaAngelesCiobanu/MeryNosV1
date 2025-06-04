package com.example.merynos

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
import com.example.merynos.R
import com.example.merynos.databinding.ActivityCartaBinding
import com.example.merynos.room.DetallePedidoEntity
import com.example.merynos.room.MesaEntity // <-- Importa MesaEntity
import com.example.merynos.room.PedidoEntity
import com.example.merynos.room.PuntosDao
import kotlinx.coroutines.launch
import java.util.Locale

class CartaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartaBinding
    private lateinit var db: AppDatabase
    private var idMesaGlobal: Int? = null // Ahora será el ID de la mesa
    private var codigoMesaQRGlobal: String? = null // Para guardar el código QR como String
    private var idUsuarioActual: Int? = null
    private var esAdmin: Boolean = false
    private lateinit var puntosDao: PuntosDao

    private val addEditCoctelActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Al volver de AddEditCoctelActivity, recargamos la carta y los puntos
                cargarDatosDeLaCarta(codigoMesaQRGlobal) // Usar el código QR global
                actualizarPuntosUsuarioUI()
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

        puntosDao = db.puntosDao()

        val rolUsuario = intent.getStringExtra("rol")
        idUsuarioActual = intent.getIntExtra("usuario_id", 0)
        if (idUsuarioActual == 0) {
            idUsuarioActual = null
        }

        esAdmin = (rolUsuario == "admin")

        val codigoMesaIntent = intent.getStringExtra("mesa") // Viene como String (el QR)

        if (esAdmin) {
            binding.txtMesa.text = "📍 Administración de Cócteles"
            binding.txtPuntosUsuario.visibility = View.GONE
            binding.btnVerCarrito.visibility = View.GONE // Admin en modo gestión no usa carrito
            binding.btnCerrarSesion.text = "Volver al Panel Admin" // Cambia el texto del botón
            cargarDatosDeLaCarta(null) // Admin ve todos los cócteles
        } else if (codigoMesaIntent != null) {
            // Convertimos el String del QR a Int para idMesaGlobal
            idMesaGlobal = codigoMesaIntent.toIntOrNull()
            if (idMesaGlobal == null) {
                Toast.makeText(this, "Error: Código de mesa inválido.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            codigoMesaQRGlobal = codigoMesaIntent // Guardamos el String del QR
            binding.txtMesa.text = "📍 $codigoMesaQRGlobal"
            binding.txtPuntosUsuario.visibility = View.VISIBLE
            actualizarPuntosUsuarioUI()
            cargarDatosDeLaCarta(codigoMesaQRGlobal)
        } else {
            Toast.makeText(this, "Error: Mesa no especificada o acceso inválido.", Toast.LENGTH_LONG).show()
            binding.txtPuntosUsuario.visibility = View.GONE
            finish()
            return
        }

        if (esAdmin) {
            binding.btnAnadirCoctelAdmin.visibility = View.VISIBLE
            binding.btnAnadirCoctelAdmin.setOnClickListener {
                val intent = Intent(this, AddEditCoctelActivity::class.java)
                addEditCoctelActivityResultLauncher.launch(intent)
            }
        } else {
            binding.btnAnadirCoctelAdmin.visibility = View.GONE
        }

        binding.btnVerCarrito.setOnClickListener {
            if (esAdmin && codigoMesaQRGlobal == null) {
                Toast.makeText(this, "Función de carrito de cliente no disponible aquí.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (idMesaGlobal == null || idUsuarioActual == null) {
                Toast.makeText(this, "No se puede acceder al carrito: información de mesa o usuario incompleta.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val pedidoPendiente = db.pedidoDao()
                    .obtenerPorMesaYEstado(idMesaGlobal!!, "pendiente")
                    .find { it.id_usuario == idUsuarioActual!! }

                val intent = Intent(this@CartaActivity, CarritoActivity::class.java)
                if (pedidoPendiente != null) {
                    intent.putExtra("ID_PEDIDO_ACTUAL", pedidoPendiente.id_pedido)
                    Log.d("CartaActivity", "Abriendo carrito para pedido ID: ${pedidoPendiente.id_pedido}")
                } else {
                    Log.d("CartaActivity", "Abriendo carrito, no hay pedido pendiente. Pasando IDs de mesa/usuario.")
                }
                intent.putExtra("ID_MESA_CARRITO", codigoMesaQRGlobal) // Pasa el código QR como String
                intent.putExtra("ID_USUARIO_CARRITO", idUsuarioActual)
                intent.putExtra("NOMBRE_MESA_CARRITO", binding.txtMesa.text.toString().substringAfter("📍").trim())
                startActivity(intent)
            }
        }

        // --- Lógica del botón "Cerrar Sesión" (o "Volver al Panel Admin") ---
        binding.btnCerrarSesion.setOnClickListener {
            if (esAdmin) {
                // Si es admin, simplemente vuelve a BarmanActivity
                val intent = Intent(this@CartaActivity, BarmanActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Si es un cliente/invitado, liberar la mesa y volver al Login
                liberarMesaYVolverLogin()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!esAdmin && idUsuarioActual != null) {
            actualizarPuntosUsuarioUI()
        } else if (esAdmin) {
            binding.txtPuntosUsuario.visibility = View.GONE
        }
        cargarDatosDeLaCarta(codigoMesaQRGlobal) // Recargar la carta al volver
    }

    // --- Función para liberar la mesa y volver al Login ---
    private fun liberarMesaYVolverLogin() {
        if (codigoMesaQRGlobal != null) {
            lifecycleScope.launch {
                try {
                    val mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaQRGlobal!!)
                    if (mesa != null && mesa.estado == "ocupada") { // Solo liberar si está ocupada
                        mesa.estado = "libre" // <-- Cambiar el estado a "libre"
                        db.mesaDao().updateMesa(mesa) // <-- Actualizar en la base de datos
                        Log.d("CartaActivity", "Mesa ${codigoMesaQRGlobal} liberada.")
                        runOnUiThread { Toast.makeText(this@CartaActivity, "Mesa ${codigoMesaQRGlobal} liberada.", Toast.LENGTH_SHORT).show() }
                    }
                } catch (e: Exception) {
                    Log.e("CartaActivity", "Error al liberar la mesa ${codigoMesaQRGlobal}: ${e.message}")
                    runOnUiThread { Toast.makeText(this@CartaActivity, "Error al liberar la mesa.", Toast.LENGTH_SHORT).show() }
                } finally {
                    // Siempre redirigir al LoginActivity, incluso si falla la liberación
                    val intent = Intent(this@CartaActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpiar la pila de actividades
                    startActivity(intent)
                    finish() // Finaliza CartaActivity
                }
            }
        } else {
            // Si no hay código de mesa (caso excepcional), simplemente vuelve al login
            val intent = Intent(this@CartaActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    // --------------------------------------------------------

    fun actualizarPuntosUsuarioUI() {
        if (idUsuarioActual != null) {
            lifecycleScope.launch {
                val puntos = obtenerPuntosUsuario()
                runOnUiThread {
                    binding.txtPuntosUsuario?.text = "Tus puntos: $puntos"
                    binding.txtPuntosUsuario.visibility = View.VISIBLE
                }
            }
        } else {
            binding.txtPuntosUsuario?.text = "Puntos: Inicia sesión"
            binding.txtPuntosUsuario.visibility = View.VISIBLE
        }
    }

    suspend fun obtenerPuntosUsuario(): Int {
        return puntosDao.obtenerTotalPuntos(idUsuarioActual ?: -1) ?: 0
    }

    private fun cargarDatosDeLaCarta(codigoMesaQR: String?) {
        lifecycleScope.launch {
            // Este bloque solo ajusta idMesaGlobal si viene un código QR
            if (codigoMesaQR != null) {
                val mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaQR)
                idMesaGlobal = mesa?.id_mesa // Asegurarse de que idMesaGlobal se actualiza aquí
                if (mesa == null && !esAdmin) { // Si no es admin y mesa no encontrada, error
                    runOnUiThread {
                        Toast.makeText(this@CartaActivity, "Mesa '$codigoMesaQR' no encontrada.", Toast.LENGTH_SHORT).show()
                    }
                    binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), false, {}, {}, {})
                    return@launch
                }
            } else if (!esAdmin) {
                // Este caso debería ser manejado por la lógica de LoginActivity
                Log.e("CartaActivity_Debug", "Error: Cliente/Invitado sin código de mesa intentando cargar carta.")
                runOnUiThread { Toast.makeText(this@CartaActivity, "Error: Mesa no especificada para el cliente.", Toast.LENGTH_LONG).show() }
                binding.recyclerCocteles.adapter = CoctelAdapter(emptyList(), false, {}, {}, {})
                return@launch
            }
            // Si es admin y codigoMesaQR es null, idMesaGlobal se mantiene null (lo cual es correcto para gestión)

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
                    else -> R.drawable.placeholder_coctel
                }
                ItemCoctel(
                    id_coctel = coctelEntity.id_coctel,
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
                        // La lógica para añadir a pedido
                        if (esAdmin) {
                            Toast.makeText(this@CartaActivity, "Función de pedido no aplicable para admin en esta vista.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idMesaGlobal == null) {
                            Toast.makeText(this@CartaActivity, "Mesa no válida para hacer pedido.", Toast.LENGTH_SHORT).show()
                            return@CoctelAdapter
                        }
                        if (idUsuarioActual == null) {
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
                                    coctelId = detalleExistente.id_coctel,
                                    nuevaCantidad = detalleExistente.cantidad + 1
                                )
                            } else {
                                db.pedidoDao().insertarDetalle(
                                    DetallePedidoEntity(
                                        id_pedido = pedidoId,
                                        id_coctel = coctelSeleccionado.id_coctel,
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
                            intent.putExtra("ID_COCTEL_A_EDITAR", coctelAEditar.id_coctel)
                            addEditCoctelActivityResultLauncher.launch(intent)
                        } else {
                            Toast.makeText(this@CartaActivity, "Acceso no autorizado para editar cócteles.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteClick = { coctelABorrar ->
                        if (esAdmin) {
                            mostrarDialogoConfirmarBorrado(coctelABorrar)
                        } else {
                            Toast.makeText(this@CartaActivity, "Acceso no autorizado para borrar cócteles.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Log.d("CartaActivity_Debug", "Adapter configurado. Tamaño de listaItems pasada al adapter: ${listaItems.size}")
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

                        cargarDatosDeLaCarta(codigoMesaQRGlobal) // Recargar la carta después de borrar
                    } else {
                        Toast.makeText(this@CartaActivity, "Error: Cóctel no encontrado para borrar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}