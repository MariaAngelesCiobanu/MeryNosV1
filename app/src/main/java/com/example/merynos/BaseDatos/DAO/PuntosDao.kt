package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy // Asegúrate de importar esto

@Dao
interface PuntosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Usar REPLACE para manejar conflictos si los hubiera
    suspend fun insertarPuntos(punto: PuntosEntity)

    // Este método ahora obtiene el 'puntosTotales' del último registro para el usuario.
    // Asume que los registros se insertan en orden cronológico.
    @Query("SELECT puntosTotales FROM Puntos WHERE id_usuario = :idUsuario ORDER BY id DESC LIMIT 1")
    suspend fun obtenerTotalPuntos(idUsuario: Int): Int?

    // --- ¡ELIMINA este método, ya que 'insertarPuntos' se usará para ambos! ---
    // suspend fun registrarCanjePuntos(punto: PuntosEntity)

    // Opcional: Para obtener el historial completo (todos los registros, incluyendo el saldo en cada momento)
    @Query("SELECT * FROM Puntos WHERE id_usuario = :idUsuario ORDER BY id DESC")
    suspend fun obtenerHistorialPuntos(idUsuario: Int): List<PuntosEntity>
}