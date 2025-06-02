package com.example.merynos // O tu paquete

import android.app.Activity // Importado para setResult, aunque no se usa directamente aquí pero sí en AddEditCoctelActivity
import android.content.Intent
import android.os.Bundle
import android.text.InputType // Para el EditText del diálogo
import android.util.Log      // Para los mensajes de Log
import android.widget.EditText // Para el EditText del diálogo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Para el diálogo emergente
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityLoginBinding
import com.example.merynos.room.MesaEntity // Necesario para crear una MesaEntity
import com.example.merynos.room.UsuarioEntity // Si no lo tenías
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    // Binding para acceder a las vistas del layout XML (activity_login.xml)
    private lateinit var binding: ActivityLoginBinding
    // Instancia de la base de datos Room
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout y establecerlo como la vista de la actividad
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar la instancia de la base de datos
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db" // Nombre del archivo de la base de datos
        )
            .fallbackToDestructiveMigration() // Si cambias el esquema, la BD se recrea (pierde datos, solo para desarrollo)
            .build()

        // Configurar el listener para el botón de "Iniciar Sesión"
        binding.btnVolverLogin.setOnClickListener {
            // Obtener el email y la contraseña de los campos de texto, quitando espacios extra
            val emailIngresado = binding.etNombre.text.toString().trim() // Asumo que etNombre es para el EMAIL
            val contraseñaIngresada = binding.etPassword.text.toString().trim()

            // Validación básica: asegurarse de que los campos no estén vacíos
            if (emailIngresado.isEmpty() || contraseñaIngresada.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Salir del listener si hay campos vacíos
            }

            // Lógica para el administrador (credenciales fijas)
            if (emailIngresado == "admin" && contraseñaIngresada == "1234") {
                Toast.makeText(this, "Admin Logueado", Toast.LENGTH_SHORT).show()
                // Si es admin, solicitar código de mesa y continuar
                solicitarCodigoMesaYContinuar(rol = "admin", usuarioId = 0) // Se podría usar un ID específico para el admin si está en la BD Usuarios
                // No llamamos a finish() aquí, se llamará después de introducir la mesa
            } else {
                // Si no es el admin hardcodeado, verificar en la base de datos para usuarios normales
                lifecycleScope.launch { // Iniciar una corutina para operaciones de base de datos (que son suspend)
                    val usuarioDesdeDb = db.usuarioDao().getPorEmail(emailIngresado) // Buscar usuario por email

                    // Volver al hilo principal para interactuar con la UI (Toasts, navegación)
                    runOnUiThread {
                        if (usuarioDesdeDb != null && usuarioDesdeDb.contraseña == contraseñaIngresada) {
                            // Usuario encontrado y contraseña correcta
                            Toast.makeText(this@LoginActivity, "Cliente Logueado", Toast.LENGTH_SHORT).show()
                            // Cliente logueado, ahora solicitar código de mesa y continuar
                            solicitarCodigoMesaYContinuar(rol = usuarioDesdeDb.rol, usuarioId = usuarioDesdeDb.id_usuario)
                            // No llamamos a finish() aquí
                        } else {
                            // Usuario no encontrado o contraseña incorrecta
                            Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Configurar el listener para el texto/botón de "Registrarse"
        binding.txtRegistro.setOnClickListener{
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
            // No se llama a finish() para permitir al usuario volver a la pantalla de login desde el registro
        }

        // Configurar el listener para el botón "Entrar como invitado"
        // Este botón ahora también solicitará la mesa.
        binding.invitadoCarta.setOnClickListener{
            Toast.makeText(this, "Acceso como Invitado", Toast.LENGTH_SHORT).show()
            // Invitado, solicitar código de mesa y continuar
            solicitarCodigoMesaYContinuar(rol = "invitado", usuarioId = 0) // Usamos 0 como ID para invitados (o un valor que no exista como ID real)
        }
    }

    /**
     * Muestra un diálogo para que el usuario ingrese el código de mesa.
     * Si la mesa no existe en la BD, la crea.
     * Luego, lanza CartaActivity con el código de mesa, rol e id de usuario.
     * @param rol El rol del usuario ("admin", "cliente", "invitado").
     * @param usuarioId El ID del usuario (puede ser 0 o un ID especial para invitados/admin no registrado).
     */
    private fun solicitarCodigoMesaYContinuar(rol: String, usuarioId: Int) {
        val builder = AlertDialog.Builder(this) // Crear el constructor del diálogo
        builder.setTitle("Ingresar Código de Mesa") // Título del diálogo
        builder.setMessage("Por favor, introduce el código de tu mesa:") // Mensaje/instrucción

        // Crear un EditText programáticamente para la entrada del usuario
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT // Tipo de entrada: texto normal
        builder.setView(input) // Añadir el EditText al diálogo

        // Configurar el botón "Aceptar" del diálogo
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val codigoMesaIngresado = input.text.toString().trim() // Obtener el texto y quitar espacios

            if (codigoMesaIngresado.isNotEmpty()) { // Verificar que se ingresó algo
                lifecycleScope.launch { // Iniciar corutina para operaciones de BD
                    var mesa = db.mesaDao().getMesaPorCodigoQR(codigoMesaIngresado) // Buscar la mesa en la BD

                    if (mesa == null) { // Si la mesa no existe...
                        try {
                            // ...intentar crearla y añadirla a la base de datos
                            db.mesaDao().insertMesa(
                                MesaEntity(
                                    codigoQR = codigoMesaIngresado,
                                    nombreMesa = codigoMesaIngresado // Usar el código como nombre por defecto
                                    // 'estado' usará el valor por defecto "libre" de MesaEntity
                                )
                            )
                            Log.i("TABLE_CREATION", "Mesa '$codigoMesaIngresado' creada y añadida a la BD.")
                        } catch (e: Exception) {
                            Log.e("TABLE_CREATION", "Error al insertar nueva mesa '$codigoMesaIngresado'", e)
                            // Si hay error al crear, mostrar Toast y no continuar a CartaActivity
                            runOnUiThread{ Toast.makeText(this@LoginActivity, "Error al crear la mesa", Toast.LENGTH_SHORT).show() }
                            return@launch
                        }
                    } else {
                        // Si la mesa ya existía, solo registrar en Log
                        Log.d("TABLE_INFO", "Mesa '$codigoMesaIngresado' ya existe en la BD.")
                    }

                    // Si todo fue bien (mesa existe o fue creada), lanzar CartaActivity
                    val intent = Intent(this@LoginActivity, CartaActivity::class.java)
                    intent.putExtra("mesa", codigoMesaIngresado) // Pasar código de mesa
                    intent.putExtra("rol", rol)                   // Pasar rol del usuario
                    intent.putExtra("usuario_id", usuarioId)      // Pasar ID del usuario
                    startActivity(intent)
                    finish() // Cerrar LoginActivity después de navegar a CartaActivity
                }
                dialog.dismiss() // Cerrar el diálogo
            } else {
                // Si no se ingresó texto, mostrar mensaje
                Toast.makeText(this, "Debes ingresar un código de mesa", Toast.LENGTH_SHORT).show()
                // Opcional: podrías volver a llamar a solicitarCodigoMesaYContinuar() aquí para que reintente.
            }
        }
        // Configurar el botón "Cancelar" del diálogo
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel() // Simplemente cierra el diálogo
            // Podrías añadir lógica aquí si quieres que pase algo más al cancelar (ej. no cerrar LoginActivity)
        }

        val alert = builder.create()
        alert.setCancelable(false) // Impedir que el diálogo se cierre al tocar fuera
        alert.setCanceledOnTouchOutside(false) // Impedir que el diálogo se cierre con el botón "atrás" del sistema (opcional)
        alert.show() // Mostrar el diálogo
    }
}