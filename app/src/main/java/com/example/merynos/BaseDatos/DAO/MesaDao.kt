package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MesaDao {
    // Método para encontrar una mesa por su código QR (String)
    @Query("SELECT * FROM Mesa WHERE codigoQR = :codigoQR LIMIT 1")
    suspend fun getMesaPorCodigoQR(codigoQR: String): MesaEntity?

    // Insertar una nueva mesa. El id_mesa LO PROPORCIONAMOS NOSOTROS.
    // OnConflictStrategy.REPLACE: Si el id_mesa (que es el codigoQR) ya existe,
    // se reemplaza la fila existente.
    @Insert(onConflict = OnConflictStrategy.REPLACE) // <-- ¡Importante!
    suspend fun insertMesa(mesa: MesaEntity) // <-- ¡Ya NO devuelve Long!

    // Obtener todas las mesas
    @Query("SELECT * FROM Mesa ORDER BY nombreMesa ASC")
    suspend fun getAllMesas(): List<MesaEntity>

    // Actualizar una mesa existente
    @Update
    suspend fun updateMesa(mesa: MesaEntity)
}