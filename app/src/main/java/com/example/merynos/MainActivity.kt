package com.example.merynos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.merynos.BaseDatos.AppDatabase
import com.example.merynos.databinding.ActivityMainBinding
import com.example.merynos.room.UsuarioEntity
import com.example.merynos.room.MesaEntity
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "merynos.db"
        ).build()

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
            }

            // Insertar Mesa 2 si no existe
            val mesa2 = db.mesaDao().getMesaPorCodigo("Mesa 2")
            if (mesa2 == null) {
                db.mesaDao().insertMesa(
                    MesaEntity(
                        codigoQR = "Mesa 2",
                        nombreMesa = "Mesa 2"
                    )
                )
            }
        }

        // Botón de invitado → escanear QR
        binding.btnInvitado.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escanea el código QR de la mesa")
            integrator.setBeepEnabled(false)
            integrator.setOrientationLocked(true)
            integrator.initiateScan()
        }

        // Botón de login
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val mesa = result.contents // por ejemplo: "Mesa 2"
                val intent = Intent(this, CartaActivity::class.java)
                intent.putExtra("mesa", mesa)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
