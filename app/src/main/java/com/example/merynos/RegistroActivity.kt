package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityRegistroBinding
import com.example.merynos.room.UsuarioEntity
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- AÑADIMOS LA VALIDACIÓN DE LA CONTRASEÑA AQUÍ ---
            val validacionContraseñaResultado = validarContraseña(pass)
            if (validacionContraseñaResultado != null) {
                Toast.makeText(this, validacionContraseñaResultado, Toast.LENGTH_LONG).show()
                return@setOnClickListener // Si la contraseña no es válida, salir del listener
            }
            // ----------------------------------------------------

            lifecycleScope.launch {
                val usuarioExistente = db.usuarioDao().getPorEmail(email)
                if (usuarioExistente != null) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    db.usuarioDao().insertUsuario(
                        UsuarioEntity(
                            nombreYApellidos = nombre,
                            email = email,
                            contraseña = pass, // Aquí se guarda la contraseña (considera encriptarla)
                            rol = "cliente"
                        )
                    )
                    runOnUiThread {
                        Toast.makeText(this@RegistroActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegistroActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }

        binding.txtIniciarSesion.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // --- FUNCIÓN PARA VALIDAR LA CONTRASEÑA ---
    /**
     * Valida si la contraseña cumple con los requisitos.
     * @return null si la contraseña es válida, o un String con el mensaje de error si no lo es.
     */
    private fun validarContraseña(password: String): String? {
        if (password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres."
        }
        if (!password.contains(Regex("[A-Z]"))) {
            return "La contraseña debe contener al menos una letra mayúscula."
        }
        if (!password.contains(Regex("[a-z]"))) {
            return "La contraseña debe contener al menos una letra minúscula."
        }
        if (!password.contains(Regex("[0-9]"))) {
            return "La contraseña debe contener al menos un número."
        }
        return null // La contraseña es válida
    }
    // ------------------------------------------------
}