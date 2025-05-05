package com.example.merynos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.merynos.databinding.ActivityCartaBinding

class CartaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartaBinding

    private val listaCocteles = listOf(
        Coctel("Mojito", "Refrescante con menta y lima", 6.00, R.drawable.placeholder_coctel),
        Coctel("Margarita", "Tequila, triple sec y lima", 7.50, R.drawable.placeholder_coctel),
        Coctel("Piña Colada", "Ron, piña y coco", 6.50, R.drawable.placeholder_coctel),
        Coctel("Cosmopolitan", "Vodka, arándano y limón", 7.00, R.drawable.placeholder_coctel)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar código de mesa
        val codigoMesa = intent.getStringExtra("mesa") ?: "Mesa desconocida"
        binding.txtMesa.text = "📍 $codigoMesa"

        // Configurar RecyclerView
        binding.recyclerCocteles.layoutManager = LinearLayoutManager(this)
        binding.recyclerCocteles.adapter = CoctelAdapter(listaCocteles)

        // Botón para volver al login
        binding.btnVolverLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
