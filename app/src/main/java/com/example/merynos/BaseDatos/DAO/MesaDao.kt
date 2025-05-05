package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MesaDao {
    @Query("SELECT * FROM Mesa WHERE codigoQR = :codigo LIMIT 1")
    suspend fun getMesaPorCodigo(codigo: String): MesaEntity?

    @Insert
    suspend fun insertMesa(mesa: MesaEntity): Long
}
