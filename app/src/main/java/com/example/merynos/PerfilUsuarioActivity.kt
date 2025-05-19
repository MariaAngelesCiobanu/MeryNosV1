package com.example.merynos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.merynos.databinding.ActivityPerfilUsuarioBinding

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilUsuarioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🧾 Obtener los datos del usuario desde el intent
        val nombre = intent.getStringExtra("nombre") ?: "Nombre desconocido"
        val correo = intent.getStringExtra("correo") ?: "Correo desconocido"

        binding.txtNombreUsuario.text = "Nombre: $nombre"
        binding.txtCorreoUsuario.text = "Correo: $correo"

        binding.btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
