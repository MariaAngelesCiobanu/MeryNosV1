package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MesaDao {
    @Query("SELECT * FROM Mesa WHERE codigoQR = :codigo LIMIT 1")
    suspend fun getMesaPorCodigo(codigo: String): MesaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMesa(mesa: MesaEntity): Long // Devuelve el rowId de la mesa insertada

    // Esta función será usada por CartaActivity para encontrar la mesa
    // basándose en el código ingresado en el diálogo (que corresponde a codigoQR)
    @Query("SELECT * FROM Mesa WHERE codigoQR = :codigoQR LIMIT 1")
    suspend fun getMesaPorCodigoQR(codigoQR: String): MesaEntity?

    @Query("SELECT * FROM Mesa ORDER BY nombreMesa ASC")
    suspend fun getAllMesas(): List<MesaEntity> // Para obtener todas las mesas

    @Update
    suspend fun updateMesa(mesa: MesaEntity) // Para actualizar una mesa existente


}
