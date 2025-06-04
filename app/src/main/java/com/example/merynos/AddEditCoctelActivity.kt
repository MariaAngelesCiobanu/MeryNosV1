package com.example.merynos // O tu paquete

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityAddEditCoctelBinding
import com.example.merynos.room.CoctelEntity
import kotlinx.coroutines.launch

class AddEditCoctelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCoctelBinding
    private lateinit var db: AppDatabase
    private var currentCoctelId: Int? = null // Para saber si estamos editando o añadiendo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCoctelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar la base de datos
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration() // Importante si cambias esquemas
            .build()

        // Comprobar si se pasó un ID de cóctel para editar
        if (intent.hasExtra("ID_COCTEL_A_EDITAR")) {
            currentCoctelId = intent.getIntExtra("ID_COCTEL_A_EDITAR", 0)
            if (currentCoctelId == 0) { // ID inválido pasado
                Toast.makeText(this, "Error: ID de cóctel inválido para editar.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            binding.tvFormCoctelTitulo.text = "Editar Cóctel"
            cargarDatosCoctelParaEdicion(currentCoctelId!!)
        } else {
            binding.tvFormCoctelTitulo.text = "Añadir Nuevo Cóctel"
            // No es necesario hacer nada más para el modo "Añadir", el formulario está vacío.
        }

        // Listener para el botón de Guardar
        binding.btnGuardarCoctelForm.setOnClickListener {
            guardarCoctel()
        }

        // --- Nuevo Listener para el botón Cancelar ---
        binding.btnCancelarCoctelForm.setOnClickListener {
            // Simplemente finaliza esta actividad, volviendo a la anterior sin guardar cambios
            finish()
        }
        // ---------------------------------------------
    }

    private fun cargarDatosCoctelParaEdicion(idCoctel: Int) {
        lifecycleScope.launch {
            val coctel = db.coctelDao().getCoctelPorId(idCoctel)
            if (coctel != null) {
                runOnUiThread {
                    binding.etNombreCoctelForm.setText(coctel.nombreCoctel)
                    binding.etHistoriaForm.setText(coctel.historia)
                    binding.etMetodoForm.setText(coctel.metodoElaboracion)
                    // Asegúrate de que el precio se muestra correctamente, a veces Double necesita un formato específico
                    binding.etPrecioForm.setText(String.format("%.2f", coctel.precioCoctel))
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@AddEditCoctelActivity, "Error al cargar datos del cóctel para editar.", Toast.LENGTH_LONG).show()
                    finish() // Cerramos si no se pudo cargar el cóctel
                }
            }
        }
    }

    private fun guardarCoctel() {
        val nombre = binding.etNombreCoctelForm.text.toString().trim()
        val historia = binding.etHistoriaForm.text.toString().trim()
        val metodo = binding.etMetodoForm.text.toString().trim()
        val precioStr = binding.etPrecioForm.text.toString().trim()

        // Validación simple
        if (nombre.isEmpty()) {
            binding.etNombreCoctelForm.error = "El nombre es obligatorio"
            return
        }
        if (metodo.isEmpty()) {
            binding.etMetodoForm.error = "El método de elaboración es obligatorio"
            return
        }
        if (precioStr.isEmpty()) {
            binding.etPrecioForm.error = "El precio es obligatorio"
            return
        }
        val precio = precioStr.toDoubleOrNull()
        if (precio == null || precio <= 0) {
            binding.etPrecioForm.error = "Precio inválido"
            return
        }

        val coctelParaGuardar = CoctelEntity(
            id_coctel = currentCoctelId ?: 0, // Si es null (añadiendo), id es 0 para autogenerar
            nombreCoctel = nombre,
            historia = historia,
            metodoElaboracion = metodo,
            precioCoctel = precio
        )

        lifecycleScope.launch {
            try {
                if (currentCoctelId != null) { // Editando
                    db.coctelDao().updateCoctel(coctelParaGuardar)
                    runOnUiThread {
                        Toast.makeText(this@AddEditCoctelActivity, "Cóctel actualizado", Toast.LENGTH_SHORT).show()
                    }
                } else { // Añadiendo
                    db.coctelDao().insertCoctel(coctelParaGuardar)
                    runOnUiThread {
                        Toast.makeText(this@AddEditCoctelActivity, "Cóctel añadido", Toast.LENGTH_SHORT).show()
                    }
                }
                setResult(Activity.RESULT_OK) // Indica a CartaActivity que la operación fue exitosa
                finish() // Volver a la actividad anterior
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@AddEditCoctelActivity, "Error al guardar cóctel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}