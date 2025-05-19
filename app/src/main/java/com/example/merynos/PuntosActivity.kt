
package com.example.merynos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.merynos.databinding.ActivityPuntosBinding

class PuntosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPuntosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuntosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simular puntos cargados (más adelante vendrán de la base de datos)
        val puntos = intent.getIntExtra("puntos", 0)

        binding.txtPuntos.text = "$puntos puntos"
        binding.txtDescuentoDisponible.text =
            if (puntos >= 100) "¡Tienes un descuento disponible!" else "No tienes descuentos disponibles"

        binding.btnVolver.setOnClickListener {
            startActivity(Intent(this, CartaActivity::class.java))
            finish()
        }
    }
}
