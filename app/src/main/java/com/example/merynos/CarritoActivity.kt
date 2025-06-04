package com.example.merynos

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.CarritoAdapter
import com.example.merynos.BaseDatos.adapter.ItemCarrito
import com.example.merynos.R
import com.example.merynos.databinding.ActivityCarritoBinding
import com.example.merynos.room.CoctelEntity
import com.example.merynos.room.PedidoEntity
import com.example.merynos.room.PuntosDao
import com.example.merynos.room.PuntosEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale

class CarritoActivity : AppCompatActivity(),
    CarritoAdapter.OnCantidadChangeListener,
    CarritoAdapter.OnEliminarCoctelListener { // <-- NUEVO: Implementar la interfaz para eliminar

    private lateinit var binding: ActivityCarritoBinding
    private lateinit var db: AppDatabase
    private var pedidoIdActual: Int? = null
    private var idMesaCarrito: Int? = null
    private var codigoMesaQRCarrito: String? = null
    private var idUsuarioCarrito: Int? = null

    private lateinit var carritoAdapter: CarritoAdapter
    private val listaItemsCarrito = mutableListOf<ItemCarrito>()
    private lateinit var puntosDao: PuntosDao
    private var cuponSeleccionado: String? = null
    private var puntosARestarPorCupon: Int? = null

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

        puntosDao = db.puntosDao()

        pedidoIdActual = intent.getIntExtra("ID_PEDIDO_ACTUAL", -1)
        if (pedidoIdActual == -1) pedidoIdActual = null

        idMesaCarrito = intent.getIntExtra("ID_MESA_CARRITO", -1)
        if (idMesaCarrito == -1) idMesaCarrito = null

        codigoMesaQRCarrito = intent.getStringExtra("NOMBRE_MESA_CARRITO")
        if (codigoMesaQRCarrito == null) {
            Log.w("CarritoActivity", "Código QR de mesa no recibido. Usando 'Desconocida'.")
            codigoMesaQRCarrito = "Desconocida"
        }

        idUsuarioCarrito = intent.getIntExtra("ID_USUARIO_CARRITO", -1)
        if (idUsuarioCarrito == -1 || idUsuarioCarrito == 0) {
            idUsuarioCarrito = null
        }

        setupRecyclerView() // Ahora el setupRecyclerView usa 'this' para el nuevo listener
        cargarDatosDelCarrito()
        actualizarPuntosUsuarioUI()

        binding.txtTituloCarrito?.text = "Carrito - $codigoMesaQRCarrito"
        binding.txtMesaCarrito?.text = "Mesa: $codigoMesaQRCarrito"
        binding.txtEstadoActualPedido?.text = "Cargando estado..."
        binding.txtTotalCarrito.text = "Total: €0.00"

        binding.btnCanjearPuntos?.setOnClickListener {
            if (idUsuarioCarrito != null) {
                if (cuponSeleccionado == null) {
                    mostrarOpcionesCanjePuntos()
                } else {
                    Toast.makeText(this, "Ya tienes un cupón aplicado. No son acumulables.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Inicia sesión para canjear puntos.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirmarPedido.setOnClickListener {
            if (pedidoIdActual == null) {
                Toast.makeText(this, "No hay pedido activo para confirmar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (listaItemsCarrito.isNotEmpty()) {
                confirmarPedido()
            } else {
                Toast.makeText(this, "El carrito está vacío.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSeguirComprando?.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarPuntosUsuarioUI()
        actualizarTotalCarrito()
    }

    fun actualizarPuntosUsuarioUI() {
        if (idUsuarioCarrito != null) {
            lifecycleScope.launch {
                val puntos = obtenerPuntosUsuario()
                runOnUiThread {
                    binding.txtPuntosUsuario?.text = "Tus puntos: $puntos"
                }
            }
        } else {
            binding.txtPuntosUsuario?.text = "Tus puntos: N/A"
        }
    }

    suspend fun obtenerPuntosUsuario(): Int {
        return puntosDao.obtenerTotalPuntos(idUsuarioCarrito ?: -1) ?: 0
    }

    suspend fun registrarTransaccionPuntos(puntosCambio: Int) {
        val idUsuario = idUsuarioCarrito ?: return

        val puntosActuales = obtenerPuntosUsuario()
        val nuevoPuntosTotales = puntosActuales + puntosCambio

        val transaccion = PuntosEntity(
            id_usuario = idUsuario,
            puntos = puntosCambio,
            puntosTotales = nuevoPuntosTotales
        )
        puntosDao.insertarPuntos(transaccion)
    }

    fun mostrarOpcionesCanjePuntos() {
        val puntosUsuarioDeferred = lifecycleScope.async { obtenerPuntosUsuario() }

        lifecycleScope.launch {
            val puntosUsuario = puntosUsuarioDeferred.await()
            val builder = AlertDialog.Builder(this@CarritoActivity)
            builder.setTitle("Canjear Puntos")

            val opciones = mutableListOf<String>()
            if (puntosUsuario >= 20) {
                opciones.add("20 puntos - Cupón 10% (compra > 20€)")
            }
            if (puntosUsuario >= 50) {
                opciones.add("50 puntos - Cupón 20% (compra > 50€)")
            }
            if (puntosUsuario >= 100) {
                opciones.add("100 puntos - Cupón 15% en cada cóctel (compra > 100€)")
            }

            if (opciones.isEmpty()) {
                builder.setMessage("No tienes suficientes puntos para canjear ningún cupón.")
                builder.setPositiveButton("Aceptar", null)
                builder.show()
            } else {
                builder.setItems(opciones.toTypedArray()) { dialog, which ->
                    lifecycleScope.launch {
                        val puntosACanjear: Int
                        val tipoCupon: String

                        when (opciones[which]) {
                            "20 puntos - Cupón 10% (compra > 20€)" -> {
                                puntosACanjear = 20
                                tipoCupon = "10_PORCIENTO"
                            }
                            "50 puntos - Cupón 20% (compra > 50€)" -> {
                                puntosACanjear = 50
                                tipoCupon = "20_PORCIENTO"
                            }
                            "100 puntos - Cupón 15% en cada cóctel (compra > 100€)" -> {
                                puntosACanjear = 100
                                tipoCupon = "15_PORCIENTO_COCTEL"
                            }
                            else -> {
                                return@launch
                            }
                        }

                        val puntosActualesAntesDeCanje = obtenerPuntosUsuario()
                        if (puntosActualesAntesDeCanje < puntosACanjear) {
                            runOnUiThread {
                                Toast.makeText(this@CarritoActivity, "Puntos insuficientes para este cupón.", Toast.LENGTH_SHORT).show()
                            }
                            dialog.dismiss()
                            return@launch
                        }

                        cuponSeleccionado = tipoCupon
                        puntosARestarPorCupon = puntosACanjear

                        runOnUiThread {
                            Toast.makeText(this@CarritoActivity, "Cupón '$tipoCupon' pre-seleccionado. Puntos se deducirán al confirmar pedido si cumple condición.", Toast.LENGTH_LONG).show()
                            actualizarTotalCarrito()
                        }
                        dialog.dismiss()
                    }
                }
                builder.setNegativeButton("Cancelar", null)
                builder.show()
            }
        }
    }

    private fun setupRecyclerView() {
        // --- CAMBIO AQUÍ: Pasar 'this' como OnEliminarCoctelListener también ---
        carritoAdapter = CarritoAdapter(listaItemsCarrito, this, this)
        binding.recyclerCarrito.layoutManager = LinearLayoutManager(this)
        binding.recyclerCarrito.adapter = carritoAdapter
    }

    private fun cargarDatosDelCarrito() {
        val nuevosItemsCarrito = mutableListOf<ItemCarrito>()

        lifecycleScope.launch {
            var pedidoParaMostrar: PedidoEntity? = null

            if (pedidoIdActual != null) {
                pedidoParaMostrar = db.pedidoDao().obtenerPedidoPorId(pedidoIdActual!!)
            } else if (idMesaCarrito != null && idUsuarioCarrito != null) {
                val idUsuarioNonNull = idUsuarioCarrito!!
                pedidoParaMostrar = db.pedidoDao()
                    .obtenerPorMesaYEstado(idMesaCarrito!!, "pendiente")
                    .find { it.id_usuario == idUsuarioNonNull }
                pedidoIdActual = pedidoParaMostrar?.id_pedido
            }

            if (pedidoParaMostrar == null || pedidoParaMostrar.estado != "pendiente") {
                runOnUiThread {
                    binding.txtTotalCarrito.text = "Total: €0.00"
                    binding.txtEstadoActualPedido?.text = if (pedidoParaMostrar != null) "Pedido ${pedidoParaMostrar.estado}" else "Carrito vacío"
                    listaItemsCarrito.clear()
                    if (::carritoAdapter.isInitialized) {
                        carritoAdapter.actualizarLista(emptyList())
                    }
                }
                return@launch
            }

            val detalles = db.pedidoDao().obtenerDetallesPedido(pedidoParaMostrar.id_pedido)
            var subtotalCalculadoGeneral = 0.0

            for (detalle in detalles) {
                val coctel: CoctelEntity? = db.coctelDao().getCoctelPorId(detalle.id_coctel)

                if (coctel != null) {
                    nuevosItemsCarrito.add(
                        ItemCarrito(
                            id_detalle_pedido = detalle.id_detalle_pedido,
                            id_coctel = detalle.id_coctel,
                            nombre = coctel.nombreCoctel ?: "Cóctel Desconocido",
                            cantidad = detalle.cantidad,
                            precioUnitario = coctel.precioCoctel ?: 0.0,
                            imagenResId = determinarImagenCoctel(coctel.nombreCoctel)
                        )
                    )
                    subtotalCalculadoGeneral += (detalle.cantidad * (coctel.precioCoctel ?: 0.0))
                } else {
                    Log.w("CarritoActivity", "No se encontró cóctel ID: ${detalle.id_coctel} para el detalle con id_detalle_pedido: ${detalle.id_detalle_pedido}")
                }
            }

            runOnUiThread {
                binding.txtEstadoActualPedido?.text = "Estado del Pedido: ${pedidoParaMostrar.estado}"
                binding.txtTotalCarrito.text = "Total: €%.2f".format(subtotalCalculadoGeneral)

                listaItemsCarrito.clear()
                listaItemsCarrito.addAll(nuevosItemsCarrito)

                if (::carritoAdapter.isInitialized) {
                    carritoAdapter.actualizarLista(listaItemsCarrito.toList())
                } else {
                    setupRecyclerView()
                }

                if (nuevosItemsCarrito.isEmpty() && detalles.isNotEmpty()) {
                    Toast.makeText(this@CarritoActivity, "Error al cargar algunos detalles de cócteles.", Toast.LENGTH_SHORT).show()
                } else if (nuevosItemsCarrito.isEmpty() && detalles.isEmpty() && pedidoParaMostrar?.estado == "pendiente") {
                    Toast.makeText(this@CarritoActivity, "El carrito está vacío.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun determinarImagenCoctel(nombreCoctel: String?): Int {
        return when (nombreCoctel?.lowercase(Locale.getDefault())) {
            "mojito" -> R.drawable.mojito_img
            "margarita" -> R.drawable.margarita_img
            "cosmopolitan" -> R.drawable.cosmopolitan_img
            "paloma" -> R.drawable.paloma_img
            "negroni" -> R.drawable.negroni_img
            "piña colada" -> R.drawable.pinia_colada_img
            else -> R.drawable.placeholder_coctel
        }
    }

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
                    val totalCompraOriginal = listaItemsCarrito.sumOf { it.cantidad * it.precioUnitario }
                    var totalFinalDescontado = totalCompraOriginal

                    var cupoFueAplicadoConExito = false

                    if (cuponSeleccionado != null && puntosARestarPorCupon != null) {
                        when (cuponSeleccionado) {
                            "10_PORCIENTO" -> {
                                if (totalCompraOriginal >= 20) {
                                    totalFinalDescontado *= 0.90
                                    cupoFueAplicadoConExito = true
                                } else {
                                    runOnUiThread { Toast.makeText(this@CarritoActivity, "Compra mínima de 20€ para el 10% no alcanzada. Cupón no aplicado.", Toast.LENGTH_LONG).show() }
                                }
                            }
                            "20_PORCIENTO" -> {
                                if (totalCompraOriginal >= 50) {
                                    totalFinalDescontado *= 0.80
                                    cupoFueAplicadoConExito = true
                                } else {
                                    runOnUiThread { Toast.makeText(this@CarritoActivity, "Compra mínima de 50€ para el 20% no alcanzada. Cupón no aplicado.", Toast.LENGTH_LONG).show() }
                                }
                            }
                            "15_PORCIENTO_COCTEL" -> {
                                if (totalCompraOriginal >= 100) {
                                    totalFinalDescontado = 0.0
                                    for (item in listaItemsCarrito) {
                                        totalFinalDescontado += item.cantidad * (item.precioUnitario * 0.85)
                                    }
                                    cupoFueAplicadoConExito = true
                                } else {
                                    runOnUiThread { Toast.makeText(this@CarritoActivity, "Compra mínima de 100€ para el 15% en cócteles no alcanzada. Cupón no aplicado.", Toast.LENGTH_LONG).show() }
                                }
                            }
                        }

                        if (cupoFueAplicadoConExito && idUsuarioCarrito != null) {
                            registrarTransaccionPuntos(-puntosARestarPorCupon!!)
                            runOnUiThread { Toast.makeText(this@CarritoActivity, "Puntos del cupón deducidos.", Toast.LENGTH_SHORT).show() }
                        }
                    }

                    db.pedidoDao().actualizarEstado(pedidoIdActual!!, "confirmado")

                    val puntosGanados = calcularPuntosPorCompra(totalCompraOriginal)
                    if (idUsuarioCarrito != null) {
                        registrarTransaccionPuntos(puntosGanados)
                        runOnUiThread {
                            Toast.makeText(this@CarritoActivity, "¡Has ganado $puntosGanados puntos!", Toast.LENGTH_LONG).show()
                        }
                    }

                    cuponSeleccionado = null
                    puntosARestarPorCupon = null

                    runOnUiThread {
                        Toast.makeText(this@CarritoActivity, "Pedido confirmado y enviado", Toast.LENGTH_LONG).show()
                        binding.txtTotalCarrito.text = "Total: €%.2f".format(totalFinalDescontado)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCantidadChanged(itemCarrito: ItemCarrito) {
        actualizarTotalCarrito()
        lifecycleScope.launch {
            itemCarrito.id_detalle_pedido?.let {
                db.pedidoDao().actualizarCantidadDetallePedido(it, itemCarrito.cantidad)
            }
        }
    }

    // --- NUEVO: Implementación del listener para eliminar cócteles ---
    override fun onEliminarCoctel(itemCarrito: ItemCarrito) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cóctel")
            .setMessage("¿Estás seguro de que quieres eliminar '${itemCarrito.nombre}' de tu pedido?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                lifecycleScope.launch {
                    // Asegurarse de que tenemos un pedido activo y el detalle a eliminar
                    if (pedidoIdActual != null && itemCarrito.id_detalle_pedido != null) {
                        try {
                            // Borrar el detalle del pedido de la base de datos
                            db.pedidoDao().eliminarDetallePedido(itemCarrito.id_detalle_pedido) // Necesitarás añadir este método en PedidoDao
                            runOnUiThread {
                                Toast.makeText(this@CarritoActivity, "'${itemCarrito.nombre}' eliminado del pedido.", Toast.LENGTH_SHORT).show()
                            }
                            // Recargar los datos del carrito para actualizar la UI
                            cargarDatosDelCarrito()
                        } catch (e: Exception) {
                            Log.e("CarritoActivity", "Error al eliminar cóctel del pedido: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@CarritoActivity, "Error al eliminar cóctel.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@CarritoActivity, "No se pudo eliminar el cóctel (detalle o pedido no encontrado).", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    // ------------------------------------------------------------------

    private fun actualizarTotalCarrito() {
        var totalSinDescuento = 0.0
        for (item in listaItemsCarrito) {
            totalSinDescuento += item.cantidad * item.precioUnitario
        }

        var totalFinal = totalSinDescuento
        if (cuponSeleccionado != null) {
            when (cuponSeleccionado) {
                "10_PORCIENTO" -> {
                    if (totalSinDescuento >= 20) totalFinal *= 0.90
                }
                "20_PORCIENTO" -> {
                    if (totalSinDescuento >= 50) totalFinal *= 0.80
                }
                "15_PORCIENTO_COCTEL" -> {
                    if (totalSinDescuento >= 100) {
                        totalFinal = 0.0
                        for (item in listaItemsCarrito) {
                            totalFinal += item.cantidad * (item.precioUnitario * 0.85)
                        }
                    }
                }
            }
        }
        binding.txtTotalCarrito.text = "Total: €%.2f".format(totalFinal)
    }

    private fun calcularPuntosPorCompra(totalCompra: Double): Int {
        return (totalCompra / 2).toInt()
    }
}