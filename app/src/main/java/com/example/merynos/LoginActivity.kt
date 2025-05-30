package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Importante para corutinas
import androidx.room.Room // Importante para Room
import com.example.merynos.BaseDatos.AppDatabase // Importa tu AppDatabase
import com.example.merynos.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch // Importante para corutinas

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase // Declara la variable para la base de datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa la instancia de la base de datos
        // Considera usar un Singleton para esto en una app más grande
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build() // Durante el desarrollo, puedes añadir .fallbackToDestructiveMigration()
        // si haces cambios frecuentes en el esquema y no te importa perder datos.

        binding.btnVolverLogin.setOnClickListener {
            val emailIngresado = binding.etNombre.text.toString().trim() // Asumo que etNombre es para el EMAIL
            val contraseñaIngresada = binding.etPassword.text.toString().trim()

            if (emailIngresado.isEmpty() || contraseñaIngresada.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lógica para el admin (puedes mantenerla)
            if (emailIngresado == "admin" && contraseñaIngresada == "1234") {
                Toast.makeText(this, "Inicio de sesión como Admin exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CartaActivity::class.java) // O tu AdminActivity
                intent.putExtra("rol", "admin")
                // intent.putExtra("mesa", "Mesa 1") // Esto parece específico del admin
                startActivity(intent)
                finish()
                return@setOnClickListener // Importante para no continuar con la verificación en DB
            }

            // Verificar en la base de datos para usuarios normales
            lifecycleScope.launch {
                val usuarioDesdeDb = db.usuarioDao().getPorEmail(emailIngresado)

                runOnUiThread { // Volver al hilo principal para mostrar Toasts y navegar
                    if (usuarioDesdeDb != null) {
                        // Usuario encontrado, ahora compara la contraseña
                        if (usuarioDesdeDb.contraseña == contraseñaIngresada) {
                            // ¡Contraseña correcta!
                            Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                            // Aquí decides a dónde va el usuario cliente
                            val intent = Intent(this@LoginActivity, CartaActivity::class.java)
                            intent.putExtra("rol", usuarioDesdeDb.rol)
                            intent.putExtra("usuario_id", usuarioDesdeDb.id_usuario)
                            // Puedes pasar más datos del usuario si los necesitas
                            startActivity(intent)
                            finish()
                        } else {
                            // Contraseña incorrecta
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Usuario no encontrado con ese email
                        Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.txtRegistro.setOnClickListener{
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
            // No llames a finish() aquí si quieres poder volver al login desde el registro
        }

        binding.invitadoCarta.setOnClickListener{
            val intent = Intent(this, CartaActivity::class.java)
            intent.putExtra("rol", "invitado") // Puedes pasar un rol de invitado
            startActivity(intent)
            // Considera si quieres llamar a finish() aquí también
        }
    }
}