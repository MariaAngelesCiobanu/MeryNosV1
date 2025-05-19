package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CoctelDao {
    @Query("SELECT * FROM Cocteles")
    suspend fun getAll(): List<CoctelEntity>

    @Query("SELECT * FROM Cocteles WHERE id_coctel = :id")
    suspend fun getCoctelPorId(id: Int): CoctelEntity

    @Query("SELECT * FROM Cocteles WHERE nombreCoctel = :nombre")
    suspend fun getPorNombre(nombre: String): CoctelEntity


    @Insert
    suspend fun insertAll(vararg cocteles: CoctelEntity)
}
