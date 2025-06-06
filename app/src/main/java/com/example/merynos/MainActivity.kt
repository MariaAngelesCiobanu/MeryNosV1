package com.example.merynos

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
import com.example.merynos.databinding.ActivityMainBinding
import com.example.merynos.room.CoctelEntity
import com.example.merynos.room.MesaEntity
import com.example.merynos.room.UsuarioEntity
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        lifecycleScope.launch {
            // Insertar barman si no existe
            val barman = db.usuarioDao().getPorEmail("barman@merynos.com")
            if (barman == null) {
                db.usuarioDao().insertUsuario(
                    UsuarioEntity(
                        nombreYApellidos = "Barman Prueba",
                        email = "barman@merynos.com",
                        contraseña = "1234",
                        rol = "barman"
                    )
                )
                Log.i("DB_INIT", "Usuario Barman de prueba insertado.")
            } else {
                Log.d("DB_INIT", "Usuario Barman ya existe.")
            }

            // --- INSERCIÓN DE MESA DE PRUEBA: Correcta con id_mesa asignado manualmente ---
            val codigoQR_mesa_prueba = "2"
            val id_mesa_prueba = codigoQR_mesa_prueba.toInt() // El ID será el mismo

            val mesaExistente = db.mesaDao().getMesaPorCodigoQR(codigoQR_mesa_prueba)
            if (mesaExistente == null) {
                db.mesaDao().insertMesa(
                    MesaEntity(
                        id_mesa = id_mesa_prueba, // Asignamos el ID directamente
                        codigoQR = codigoQR_mesa_prueba,
                        nombreMesa = "Mesa Principal $codigoQR_mesa_prueba",
                        estado = "libre"
                    )
                )
                Log.i("DB_INIT", "Mesa '$codigoQR_mesa_prueba' (ID: $id_mesa_prueba) de prueba insertada.")
            } else {
                Log.d("DB_INIT", "Mesa '$codigoQR_mesa_prueba' ya existe.")
            }
            // -----------------------------------------------------------------------

            poblarCoctelesDePrueba()
        }

        binding.btnInvitado.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escanea el código QR de la mesa")
            integrator.setBeepEnabled(false)
            integrator.setOrientationLocked(true)
            integrator.initiateScan()
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)) // <--- SIN @
        }

        binding.btnEntradaManualMesa.setOnClickListener {
            mostrarDialogoEntradaManualMesa()
        }
    }

    private fun mostrarDialogoEntradaManualMesa() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingresar Código de Mesa")
        builder.setMessage("Por favor, introduce el código de tu mesa:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            val codigoMesaStr = input.text.toString().trim()

            if (codigoMesaStr.isEmpty()) {
                Toast.makeText(this, "Debes ingresar un código de mesa", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val intent = Intent(this, LoginActivity::class.java) // <--- SIN @
            intent.putExtra("mesa_qr", codigoMesaStr)
            intent.putExtra("rol", "invitado")
            intent.putExtra("usuario_id", 0)
            startActivity(intent)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val mesaCodigoQR = result.contents
                Log.d("QR_SCAN", "QR Escaneado: $mesaCodigoQR")

                val intent = Intent(this, LoginActivity::class.java) // <--- SIN @
                intent.putExtra("mesa_qr", mesaCodigoQR)
                intent.putExtra("rol", "invitado")
                intent.putExtra("usuario_id", 0)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun poblarCoctelesDePrueba() {
        val coctelDao = db.coctelDao()

        if (coctelDao.getAll().isEmpty()) {
            Log.d("DB_POBLAR", "Tabla Cocteles vacía. Insertando datos de prueba...")
            try {
                val coctel1 = CoctelEntity(
                    nombreCoctel = "Mojito",
                    historia = "Nacido en las soleadas playas de Cuba, el Mojito evoca el espíritu caribeño con cada sorbo. Se dice que era el favorito del escritor Ernest Hemingway durante sus años en La Habana, disfrutándolo en La Bodeguita del Medio. Su frescura es legendaria.",
                    metodoElaboracion = "Macerar suavemente hierbabuena con azúcar y zumo de lima. Añadir ron blanco de calidad, llenar el vaso con hielo picado y completar con soda. Decorar con una ramita de hierbabuena y una rodaja de lima.",
                    precioCoctel = 7.50
                )
                val coctel2 = CoctelEntity(
                    nombreCoctel = "Margarita",
                    historia = "Un clásico indiscutible con origen en la efervescente frontera entre México y Estados Unidos, rodeado de múltiples leyendas sobre su creación para una dama llamada Margarita. Este cóctel es sinónimo de fiesta y celebración.",
                    metodoElaboracion = "Combinar en una coctelera con hielo: tequila 100% agave, Cointreau (o triple seco de calidad) y zumo de lima fresco. Agitar bien y colar en una copa de margarita previamente escarchada con sal en el borde.",
                    precioCoctel = 8.00
                )
                val coctel3 = CoctelEntity(
                    nombreCoctel = "Dry Martini",
                    historia = "Símbolo de sofisticación y elegancia atemporal, el Dry Martini es más que un cóctel; es una declaración de principios. Popularizado por la literatura y el cine, su preparación es un arte que busca el equilibrio perfecto, siendo objeto de debate entre los bartenders más puristas.",
                    metodoElaboracion = "Enfriar un vaso mezclador con hielo. Descartar el agua derretida. Añadir una pequeña porción de vermut seco, remover para cubrir el hielo y descartar el exceso de vermut (o conservarlo, según preferencia). Añadir ginebra London Dry de alta calidad. Remover con elegancia (no agitar, a menos que se pida 'estilo Bond'). Colar en una copa de martini previamente enfriada. Decorar con una aceituna verde o un twist de piel de limón.",
                    precioCoctel = 9.00
                )
                val coctel4 = CoctelEntity(
                    nombreCoctel = "Cosmopolitan",
                    historia = "Un ícono de los años 90, el Cosmopolitan ganó fama mundial gracias a series de televisión como 'Sexo en Nueva York'. Su color vibrante y su sabor agridulce lo convirtieron en un favorito instantáneo en bares de todo el mundo, representando un estilo de vida moderno y chic.",
                    metodoElaboracion = "Enfriar una copa de martini. En una coctelera con hielo, añadir vodka Citron (o vodka neutro y un chorrito de zumo de limón), Cointreau, zumo de arándanos rojos frescos y un toque de zumo de lima fresco. Agitar vigorosamente y colar en la copa fría. Decorar con un twist de naranja o una rodaja de lima.",
                    precioCoctel = 8.50
                )

                coctelDao.insertAll(coctel1, coctel2, coctel3, coctel4)
                Log.i("DB_POBLAR", "¡Cócteles de prueba (con historias largas) insertados exitosamente!")

            } catch (e: Exception) {
                Log.e("DB_POBLAR", "Error al insertar cócteles de prueba", e)
            }
        } else {
            Log.d("DB_POBLAR", "La tabla Cocteles ya contiene datos.")
        }
    }
}