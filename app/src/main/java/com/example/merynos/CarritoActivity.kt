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

class CarritoActivity : AppCompatActivity(), CarritoAdapter.OnCantidadChangeListener {

    private lateinit var binding: ActivityCarritoBinding
    private lateinit var db: AppDatabase
    private var pedidoIdActual: Int? = null
    private var idMesaCarrito: Int? = null // <-- Ahora será el ID numérico de la mesa
    private var codigoMesaQRCarrito: String? = null // <-- Nuevo campo para el código QR (String)
    private var idUsuarioCarrito: Int? = null
    // private lateinit var nombreMesaCarrito: String // Ya no es necesario si usas codigoMesaQRCarrito

    private lateinit var carritoAdapter: CarritoAdapter
    private val listaItemsCarrito = mutableListOf<ItemCarrito>()
    private lateinit var puntosDao: PuntosDao
    private var cuponSeleccionado: String? = null

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

        // --- CAMBIOS AQUÍ: Recibir el ID numérico y el String del QR ---
        idMesaCarrito = intent.getIntExtra("ID_MESA_CARRITO", -1)
        if (idMesaCarrito == -1) idMesaCarrito = null

        codigoMesaQRCarrito = intent.getStringExtra("NOMBRE_MESA_CARRITO") // Se usó este extra para pasar el QR
        if (codigoMesaQRCarrito == null) {
            // Manejar caso donde el QR no se pasa (ej. si vienes de un lugar que no lo envía)
            Log.w("CarritoActivity", "Código QR de mesa no recibido. Usando 'Desconocida'.")
            codigoMesaQRCarrito = "Desconocida"
        }
        // ----------------------------------------------------------------

        idUsuarioCarrito = intent.getIntExtra("ID_USUARIO_CARRITO", -1)
        if (idUsuarioCarrito == -1 || idUsuarioCarrito == 0) {
            idUsuarioCarrito = null
        }

        // nombreMesaCarrito = intent.getStringExtra("NOMBRE_MESA_CARRITO") ?: "Mesa Desconocida" // Ya no se usa directamente

        setupRecyclerView()
        cargarDatosDelCarrito()
        actualizarPuntosUsuarioUI()

        binding.txtTituloCarrito?.text = "Carrito - $codigoMesaQRCarrito" // Usar el código QR para el título
        binding.txtMesaCarrito?.text = "Mesa: $codigoMesaQRCarrito" // Usar el código QR para la mesa
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

                        registrarTransaccionPuntos(-puntosACanjear)

                        cuponSeleccionado = tipoCupon

                        runOnUiThread {
                            Toast.makeText(this@CarritoActivity, "Cupón aplicado: $tipoCupon. Puntos restados.", Toast.LENGTH_SHORT).show()
                            actualizarPuntosUsuarioUI()
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
        carritoAdapter = CarritoAdapter(listaItemsCarrito, this)
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
                    .obtenerPorMesaYEstado(idMesaCarrito!!, "pendiente") // Usar idMesaCarrito (Int)
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
                    db.pedidoDao().actualizarEstado(pedidoIdActual!!, "confirmado")

                    val totalCompraActual = listaItemsCarrito.sumOf { it.cantidad * it.precioUnitario }
                    val puntosObtenidos = calcularPuntosPorCompra(totalCompraActual)

                    if (idUsuarioCarrito != null) {
                        registrarTransaccionPuntos(puntosObtenidos)
                        runOnUiThread {
                            Toast.makeText(this@CarritoActivity, "¡Has ganado $puntosObtenidos puntos!", Toast.LENGTH_LONG).show()
                        }
                    }

                    cuponSeleccionado = null

                    runOnUiThread {
                        Toast.makeText(this@CarritoActivity, "Pedido confirmado y enviado", Toast.LENGTH_LONG).show()
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
                    else Toast.makeText(this, "Compra mínima de 20€ para el 10% no alcanzada.", Toast.LENGTH_SHORT).show()
                }
                "20_PORCIENTO" -> {
                    if (totalSinDescuento >= 50) totalFinal *= 0.80
                    else Toast.makeText(this, "Compra mínima de 50€ para el 20% no alcanzada.", Toast.LENGTH_SHORT).show()
                }
                "15_PORCIENTO_COCTEL" -> {
                    if (totalSinDescuento >= 100) {
                        totalFinal = 0.0
                        for (item in listaItemsCarrito) {
                            totalFinal += item.cantidad * (item.precioUnitario * 0.85)
                        }
                    } else Toast.makeText(this, "Compra mínima de 100€ para el 15% en cócteles no alcanzada.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.txtTotalCarrito.text = "Total: €%.2f".format(totalFinal)
    }

    private fun calcularPuntosPorCompra(totalCompra: Double): Int {
        return (totalCompra / 2).toInt()
    }
}