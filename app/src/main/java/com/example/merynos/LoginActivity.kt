package com.example.merynos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityLoginBinding
import com.example.merynos.room.MesaEntity
import com.example.merynos.room.UsuarioEntity // Asegúrate de que esta importación sea correcta si tienes UsuarioEntity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        binding.btnVolverLogin.setOnClickListener {
            val emailIngresado = binding.etNombre.text.toString().trim()
            val contraseñaIngresada = binding.etPassword.text.toString().trim()

            if (emailIngresado.isEmpty() || contraseñaIngresada.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lógica para el administrador (credenciales fijas)
            if (emailIngresado == "admin" && contraseñaIngresada == "1234") {
                Toast.makeText(this, "Admin Logueado", Toast.LENGTH_SHORT).show()
                // Para el admin, navegamos directamente a su actividad (BarmanActivity)
                val intent = Intent(this@LoginActivity, BarmanActivity::class.java)
                intent.putExtra("rol", "admin")
                intent.putExtra("usuario_id", 0) // Puedes usar un ID de admin predefinido
                startActivity(intent)
                finish() // Cerrar LoginActivity
            } else {
                // Si no es el admin, verificar en la base de datos para usuarios normales
                lifecycleScope.launch {
                    val usuarioDesdeDb = db.usuarioDao().getPorEmail(emailIngresado)

                    runOnUiThread {
                        if (usuarioDesdeDb != null && usuarioDesdeDb.contraseña == contraseñaIngresada) {
                            Toast.makeText(this@LoginActivity, "Cliente Logueado", Toast.LENGTH_SHORT).show()
                            // Cliente logueado, solicitar código de mesa y continuar
                            solicitarCodigoMesaYContinuar(rol = usuarioDesdeDb.rol, usuarioId = usuarioDesdeDb.id_usuario)
                        } else {
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.txtRegistro.setOnClickListener{
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        binding.invitadoCarta.setOnClickListener{
            Toast.makeText(this, "Acceso como Invitado", Toast.LENGTH_SHORT).show()
            // Invitado, solicitar código de mesa y continuar
            solicitarCodigoMesaYContinuar(rol = "invitado", usuarioId = 0)
        }
    }

    /**
     * Muestra un diálogo para que el usuario ingrese el código de mesa.
     * Si la mesa no existe en la BD, la crea y la marca como "ocupada".
     * Si la mesa existe, la actualiza a "ocupada" si no lo está ya.
     * Luego, lanza CartaActivity con el código de mesa, rol e id de usuario.
     * @param rol El rol del usuario ("admin", "cliente", "invitado").
     * @param usuarioId El ID del usuario (puede ser 0 o un ID especial para invitados/admin no registrado).
     */
    private fun solicitarCodigoMesaYContinuar(rol: String, usuarioId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingresar Código de Mesa")
        builder.setMessage("Por favor, introduce el código de tu mesa:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val codigoMesaIngresado = input.text.toString().trim()

            if (codigoMesaIngresado.isNotEmpty()) {
                lifecycleScope.launch {
                    var mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaIngresado)

                    if (mesa == null) {
                        try {
                            // Si la mesa NO existe, la creamos y la marcamos como "ocupada"
                            val nuevaMesa = MesaEntity(
                                codigoQR = codigoMesaIngresado,
                                nombreMesa = codigoMesaIngresado,
                                estado = "ocupada" // <-- ¡Aquí se establece el estado "ocupada"!
                            )
                            db.mesaDao().insertMesa(nuevaMesa)
                            mesa = nuevaMesa // Asignamos la nueva mesa a la variable 'mesa' para el siguiente paso
                            Log.i("TABLE_CREATION", "Mesa '$codigoMesaIngresado' creada y añadida a la BD en estado 'ocupada'.")
                        } catch (e: Exception) {
                            Log.e("TABLE_CREATION", "Error al insertar nueva mesa '$codigoMesaIngresado'", e)
                            runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al crear la mesa", Toast.LENGTH_SHORT).show() }
                            return@launch
                        }
                    } else {
                        // Si la mesa YA existe, la actualizamos a "ocupada" si no lo está ya
                        if (mesa.estado != "ocupada") {
                            mesa.estado = "ocupada" // <-- ¡Aquí se actualiza el estado a "ocupada"!
                            try {
                                db.mesaDao().updateMesa(mesa) // <-- Guardamos el cambio en la base de datos
                                Log.d("TABLE_UPDATE", "Mesa '$codigoMesaIngresado' ya existe, actualizada a estado 'ocupada'.")
                            } catch (e: Exception) {
                                Log.e("TABLE_UPDATE", "Error al actualizar estado de mesa '$codigoMesaIngresado'", e)
                                runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al actualizar la mesa", Toast.LENGTH_SHORT).show() }
                                return@launch
                            }
                        } else {
                            Log.d("TABLE_INFO", "Mesa '$codigoMesaIngresado' ya existe y ya está en estado 'ocupada'.")
                        }
                    }

                    // Si todo fue bien (mesa encontrada o creada y estado actualizado), lanzar CartaActivity
                    val intent = Intent(this@LoginActivity, CartaActivity::class.java)
                    intent.putExtra("mesa", codigoMesaIngresado)
                    intent.putExtra("rol", rol)
                    intent.putExtra("usuario_id", usuarioId)
                    startActivity(intent)
                    finish() // Cerrar LoginActivity después de navegar a CartaActivity
                }
                dialog.dismiss() // Cerrar el diálogo
            } else {
                Toast.makeText(this, "Debes ingresar un código de mesa", Toast.LENGTH_SHORT).show()
                // Opcional: podrías volver a llamar a solicitarCodigoMesaYContinuar() aquí para que reintente.
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        val alert = builder.create()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
}