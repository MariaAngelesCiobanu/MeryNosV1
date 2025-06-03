package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PuntosDao {

    @Insert
    suspend fun insertarPuntos(punto: PuntosEntity)

    @Query("SELECT SUM(puntosTotales) FROM Puntos WHERE id_usuario = :idUsuario")
    suspend fun obtenerTotalPuntos(idUsuario: Int): Int?

    @Insert
    suspend fun registrarCanjePuntos(punto: PuntosEntity) // Para registrar el canje

    // Opcional: Para obtener el historial de puntos
    @Query("SELECT * FROM Puntos WHERE id_usuario = :idUsuario ORDER BY id DESC")
    suspend fun obtenerHistorialPuntos(idUsuario: Int): List<PuntosEntity>
}