package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CoctelDao {
    @Query("SELECT * FROM Cocteles")
    suspend fun getAll(): List<CoctelEntity>

    @Insert
    suspend fun insertAll(vararg cocteles: CoctelEntity)
}
