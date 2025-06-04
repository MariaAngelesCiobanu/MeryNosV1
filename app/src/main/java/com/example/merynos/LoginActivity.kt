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
import com.example.merynos.room.UsuarioEntity
import com.google.zxing.integration.android.IntentIntegrator
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

            if (emailIngresado == "admin" && contraseñaIngresada == "1234") {
                Toast.makeText(this, "Admin Logueado", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@LoginActivity, BarmanActivity::class.java)
                intent.putExtra("rol", "admin")
                intent.putExtra("usuario_id", 0)
                startActivity(intent)
                finish()
            } else {
                lifecycleScope.launch {
                    val usuarioDesdeDb = db.usuarioDao().getPorEmail(emailIngresado)

                    runOnUiThread {
                        if (usuarioDesdeDb != null && usuarioDesdeDb.contraseña == contraseñaIngresada) {
                            Toast.makeText(this@LoginActivity, "Cliente Logueado", Toast.LENGTH_SHORT).show()
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
            solicitarCodigoMesaYContinuar(rol = "invitado", usuarioId = 0)
        }
    }

    private fun solicitarCodigoMesaYContinuar(rol: String, usuarioId: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingresar Código de Mesa")
        builder.setMessage("Por favor, introduce el código de tu mesa (número):") // Indicamos que debe ser un número

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER // Forzar entrada numérica
        builder.setView(input)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val codigoMesaStr = input.text.toString().trim()

            if (codigoMesaStr.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un código de mesa", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val idMesa: Int? = codigoMesaStr.toIntOrNull() // Intentamos convertir a Int
            if (idMesa == null || idMesa <= 0) { // Validamos que sea un número válido y positivo
                Toast.makeText(this, "El código de mesa debe ser un número entero positivo.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch {
                var mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaStr) // Buscar la mesa por su QR/ID

                if (mesa == null) {
                    try {
                        // Si la mesa NO existe, la creamos con id_mesa = codigoQR y estado "ocupada"
                        val nuevaMesa = MesaEntity(
                            id_mesa = idMesa, // <-- ¡Línea 116 (aprox): AQUÍ SE PASA EL VALOR!
                            codigoQR = codigoMesaStr,
                            nombreMesa = "Mesa $codigoMesaStr",
                            estado = "ocupada"
                        )
                        db.mesaDao().insertMesa(nuevaMesa) // <-- Línea 126 (aprox): insertMesa ahora no devuelve Long
                        mesa = nuevaMesa // Asignamos la nueva mesa (ya tiene el ID correcto)
                        Log.i("TABLE_CREATION", "Mesa '$codigoMesaStr' (ID: $idMesa) creada y puesta como ocupada.")
                    } catch (e: Exception) {
                        Log.e("TABLE_CREATION", "Error al insertar nueva mesa '$codigoMesaStr' (ID: $idMesa)", e)
                        runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al crear la mesa", Toast.LENGTH_SHORT).show() }
                        return@launch
                    }
                } else {
                    // Si la mesa YA existe, la actualizamos a "ocupada" si no lo está
                    if (mesa.estado != "ocupada") {
                        mesa.estado = "ocupada"
                        try {
                            db.mesaDao().updateMesa(mesa)
                            Log.d("TABLE_UPDATE", "Mesa '$codigoMesaStr' (ID: $idMesa) actualizada a estado 'ocupada'.")
                        } catch (e: Exception) {
                            Log.e("TABLE_UPDATE", "Error al actualizar estado de mesa '$codigoMesaStr' (ID: $idMesa)", e)
                            runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al actualizar la mesa", Toast.LENGTH_SHORT).show() }
                            return@launch
                        }
                    } else {
                        Log.d("TABLE_INFO", "Mesa '$codigoMesaStr' (ID: $idMesa) ya existe y ya está ocupada.")
                    }
                }

                // Navegar a CartaActivity, pasando el ID de la mesa (que es el mismo que el QR numérico)
                val intent = Intent(this@LoginActivity, CartaActivity::class.java)
                intent.putExtra("mesa_qr", codigoMesaStr) // Pasamos el QR (String)
                intent.putExtra("mesa_id", idMesa) // Pasamos el ID (Int)
                intent.putExtra("rol", rol)
                intent.putExtra("usuario_id", usuarioId)
                startActivity(intent)
                finish()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        val alert = builder.create()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val mesaCodigoQR = result.contents
                val idMesaQR: Int? = mesaCodigoQR.toIntOrNull() // Convertir a Int
                if (idMesaQR == null || idMesaQR <= 0) {
                    Toast.makeText(this, "Código QR inválido. Debe ser un número de mesa válido.", Toast.LENGTH_LONG).show()
                    return // No continuar si el QR no es un número válido
                }

                lifecycleScope.launch {
                    var mesa = db.mesaDao().getMesaPorCodigoQR(mesaCodigoQR)

                    if (mesa == null) {
                        try {
                            val nuevaMesa = MesaEntity(
                                id_mesa = idMesaQR, // <-- Aquí se pasa el valor de id_mesa!
                                codigoQR = mesaCodigoQR,
                                nombreMesa = "Mesa $mesaCodigoQR",
                                estado = "ocupada"
                            )
                            db.mesaDao().insertMesa(nuevaMesa)
                            mesa = nuevaMesa
                            Log.i("TABLE_CREATION", "Mesa QR '$mesaCodigoQR' (ID: $idMesaQR) creada y puesta como ocupada.")
                        } catch (e: Exception) {
                            Log.e("TABLE_CREATION", "Error al crear mesa QR '$mesaCodigoQR' (ID: $idMesaQR)", e)
                            runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al crear la mesa desde QR.", Toast.LENGTH_SHORT).show() }
                            return@launch
                        }
                    } else {
                        if (mesa.estado != "ocupada") {
                            mesa.estado = "ocupada"
                            try {
                                db.mesaDao().updateMesa(mesa)
                                Log.d("TABLE_UPDATE", "Mesa QR '$mesaCodigoQR' (ID: $idMesaQR) actualizada a estado 'ocupada'.")
                            } catch (e: Exception) {
                                Log.e("TABLE_UPDATE", "Error al actualizar mesa QR '$mesaCodigoQR' (ID: $idMesaQR)", e)
                                runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al actualizar la mesa desde QR.", Toast.LENGTH_SHORT).show() }
                                return@launch
                            }
                        } else {
                            Log.d("TABLE_INFO", "Mesa QR '$mesaCodigoQR' (ID: $idMesaQR) ya está ocupada.")
                        }
                    }

                    val intent = Intent(this@LoginActivity, CartaActivity::class.java)
                    intent.putExtra("mesa_qr", mesaCodigoQR)
                    intent.putExtra("mesa_id", idMesaQR)
                    intent.putExtra("rol", "invitado")
                    intent.putExtra("usuario_id", 0)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}