package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.BaseDatos.adapter.BarmanAdapter // <-- Usamos tu BarmanAdapter
import com.example.merynos.databinding.ActivityBarmanBinding // Asegúrate de que este binding es correcto
import com.example.merynos.room.PedidoEntity
import kotlinx.coroutines.launch

class BarmanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarmanBinding
    private lateinit var db: AppDatabase
    private lateinit var barmanAdapter: BarmanAdapter // <-- Usamos tu BarmanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarmanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar la base de datos Room
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).fallbackToDestructiveMigration().build()

        setupRecyclerView() // <-- Configurar el RecyclerView

        // --- ¡LÓGICA CLAVE! Observar los pedidos confirmados en tiempo real ---
        db.pedidoDao().obtenerPedidosConfirmadosParaBarman().observe(this, Observer { pedidos ->
            // Este bloque se ejecuta cada vez que hay un cambio en la base de datos
            // que coincide con la consulta 'obtenerPedidosConfirmadosParaBarman()'.
            Log.d("BarmanActivity", "LiveData activado: ${pedidos.size} pedidos confirmados recibidos.")
            barmanAdapter = BarmanAdapter(pedidos) { pedidoSeleccionado -> // Inicializamos el adaptador aquí
                mostrarDialogoCambiarEstado(pedidoSeleccionado)
            }
            binding.recyclerPedidosBarman.adapter = barmanAdapter // Asignamos el adaptador al RecyclerView
            //barmanAdapter.notifyDataSetChanged() // Notificamos los cambios al adaptador (ya no es necesario)
        })
        // ---------------------------------------------------------------------

        // Configurar el listener para el botón "Gestionar Cócteles"
        binding.btnGestionarCocteles.setOnClickListener {
            val intent = Intent(this, CartaActivity::class.java).apply {
                putExtra("rol", "admin") // O "barman"
                putExtra("gestion_modo", true)
            }
            startActivity(intent)
        }

        binding.btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- Nuevo método para configurar el RecyclerView ---
    private fun setupRecyclerView() {
        binding.recyclerPedidosBarman.layoutManager = LinearLayoutManager(this)
        // El adaptador se inicializa dentro del Observer de LiveData
    }
    // ---------------------------------------------------

    private fun mostrarDialogoCambiarEstado(pedido: PedidoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Cambiar Estado del Pedido")
            .setMessage("¿Qué estado quieres aplicar al pedido #${pedido.id_pedido}?")
            .setPositiveButton("Marcar como 'Preparado'") { dialog, _ ->
                lifecycleScope.launch {
                    db.pedidoDao().actualizarEstado(pedido.id_pedido, "preparado")
                    runOnUiThread { Toast.makeText(this@BarmanActivity, "Pedido #${pedido.id_pedido} marcado como 'preparado'", Toast.LENGTH_SHORT).show() }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Marcar como 'En Preparación'") { dialog, _ ->
                lifecycleScope.launch {
                    db.pedidoDao().actualizarEstado(pedido.id_pedido, "en_preparacion")
                    runOnUiThread { Toast.makeText(this@BarmanActivity, "Pedido #${pedido.id_pedido} marcado como 'en preparación'", Toast.LENGTH_SHORT).show() }
                }
                dialog.dismiss()
            }
            .setNeutralButton("Cancelar", null)
            .show()
    }
}