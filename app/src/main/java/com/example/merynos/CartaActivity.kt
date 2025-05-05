package com.example.merynos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityCartaBinding
import com.example.merynos.room.CoctelEntity
import kotlinx.coroutines.launch

class CartaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar código de mesa
        val codigoMesa = intent.getStringExtra("mesa") ?: "Mesa desconocida"
        binding.txtMesa.text = "📍 $codigoMesa"

        // Configurar RecyclerView
        binding.recyclerCocteles.layoutManager = LinearLayoutManager(this)

        // Cargar datos desde Room
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        lifecycleScope.launch {
            val cocteles = db.coctelDao().getAll()

            // Adaptar CoctelEntity a modelo visual (Coctel)
            val adaptados = cocteles.map {
                Coctel(
                    nombre = it.nombreCoctel,
                    descripcion = it.metodoElaboracion,
                    precio = it.precioCoctel,
                    imagenResId = R.drawable.placeholder_coctel
                )
            }

            binding.recyclerCocteles.adapter = CoctelAdapter(adaptados)
        }

        // Botón para volver al login
        binding.btnVolverLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
