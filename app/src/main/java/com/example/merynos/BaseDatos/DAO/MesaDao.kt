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

    // Usaremos REPLACE para que si insertamos una mesa con un ID existente, se reemplace.
    // Esto es útil si una mesa se "elimina" y se vuelve a crear con el mismo QR/ID.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesa(mesa: MesaEntity) // Ya no devuelve Long

    @Query("SELECT * FROM Mesa WHERE codigoQR = :codigoQR LIMIT 1")
    suspend fun getMesaPorCodigoQR(codigoQR: String): MesaEntity?

    @Query("SELECT * FROM Mesa ORDER BY nombreMesa ASC")
    suspend fun getAllMesas(): List<MesaEntity>

    @Update
    suspend fun updateMesa(mesa: MesaEntity)
}