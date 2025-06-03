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
import java.util.Date
import java.util.Locale

class CarritoActivity : AppCompatActivity(), CarritoAdapter.OnCantidadChangeListener {

    private lateinit var binding: ActivityCarritoBinding
    private lateinit var db: AppDatabase
    private var pedidoIdActual: Int? = null
    private var idMesaCarrito: Int? = null
    private var idUsuarioCarrito: Int? = null
    private lateinit var nombreMesaCarrito: String

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

        pedidoIdActual = if (intent.hasExtra("ID_PEDIDO_ACTUAL")) intent.getIntExtra("ID_PEDIDO_ACTUAL", -1) else null
        if (pedidoIdActual == -1) pedidoIdActual = null

        idMesaCarrito = if (intent.hasExtra("ID_MESA_CARRITO")) intent.getIntExtra("ID_MESA_CARRITO", -1) else null
        if (idMesaCarrito == -1) idMesaCarrito = null

        idUsuarioCarrito = if (intent.hasExtra("ID_USUARIO_CARRITO")) intent.getIntExtra("ID_USUARIO_CARRITO", -1) else null
        if (idUsuarioCarrito == -1 && idUsuarioCarrito != 0) idUsuarioCarrito = null

        nombreMesaCarrito = intent.getStringExtra("NOMBRE_MESA_CARRITO") ?: "Mesa Desconocida"

        setupRecyclerView()
        cargarDatosDelCarrito()
        actualizarPuntosUsuarioUI()

        binding.txtTituloCarrito?.text = "Carrito - $nombreMesaCarrito"
        binding.txtMesaCarrito?.text = "Mesa: $nombreMesaCarrito"
        binding.txtEstadoActualPedido?.text = "Cargando estado..."
        binding.txtTotalCarrito.text = "Total: €0.00"

        binding.btnCanjearPuntos?.setOnClickListener {
            mostrarOpcionesCanjePuntos()
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
    }

    fun actualizarPuntosUsuarioUI() {
        lifecycleScope.launch {
            val puntos = obtenerPuntosUsuario()
            binding.txtPuntosUsuario?.text = "Tus puntos: $puntos"
        }
    }

    suspend fun obtenerPuntosUsuario(): Int {
        return puntosDao.obtenerTotalPuntos(idUsuarioCarrito ?: -1) ?: 0
    }

    suspend fun registrarCanje(puntosCanjeados: Int) {
        val puntosActuales = obtenerPuntosUsuario()
        val canje = PuntosEntity(
            id_usuario = idUsuarioCarrito ?: -1,
            puntos = -puntosCanjeados,
            puntosTotales = puntosActuales - puntosCanjeados
        )
        puntosDao.registrarCanjePuntos(canje)
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
                        when (opciones[which]) {
                            "20 puntos - Cupón 10% (compra > 20€)" -> {
                                cuponSeleccionado = "10_PORCIENTO"
                                registrarCanje(20)
                                actualizarTotalCarrito()
                                actualizarPuntosUsuarioUI()
                                Toast.makeText(this@CarritoActivity, "Cupón del 10% aplicado.", Toast.LENGTH_SHORT).show()
                            }
                            "50 puntos - Cupón 20% (compra > 50€)" -> {
                                cuponSeleccionado = "20_PORCIENTO"
                                registrarCanje(50)
                                actualizarTotalCarrito()
                                actualizarPuntosUsuarioUI()
                                Toast.makeText(this@CarritoActivity, "Cupón del 20% aplicado.", Toast.LENGTH_SHORT).show()
                            }
                            "100 puntos - Cupón 15% en cada cóctel (compra > 100€)" -> {
                                cuponSeleccionado = "15_PORCIENTO_COCTEL"
                                registrarCanje(100)
                                actualizarTotalCarrito()
                                actualizarPuntosUsuarioUI()
                                Toast.makeText(this@CarritoActivity, "Cupón del 15% en cada cóctel aplicado.", Toast.LENGTH_SHORT).show()
                            }
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
                    // Aquí podrías enviar el cuponSeleccionado con la información del pedido
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
                "10_PORCIENTO" -> if (totalSinDescuento > 20) totalFinal *= 0.90
                "20_PORCIENTO" -> if (totalSinDescuento > 50) totalFinal *= 0.80
                "15_PORCIENTO_COCTEL" -> if (totalSinDescuento > 100) {
                    totalFinal = 0.0
                    for (item in listaItemsCarrito) {
                        totalFinal += item.cantidad * (item.precioUnitario * 0.85)
                    }
                }
            }
        }
        binding.txtTotalCarrito.text = "Total: €%.2f".format(totalFinal)
    }
}