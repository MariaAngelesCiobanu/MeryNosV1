package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Delete // Importa @Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy // Importa OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // Importa @Update

@Dao
interface CoctelDao {

    @Query("SELECT * FROM Cocteles ORDER BY nombreCoctel ASC")
    suspend fun getAll(): List<CoctelEntity>

    @Query("SELECT * FROM Cocteles WHERE id_coctel = :id")
    suspend fun getCoctelPorId(id: Int): CoctelEntity? // <-- CAMBIADO a nulable (CoctelEntity?)

    @Query("SELECT * FROM Cocteles WHERE nombreCoctel = :nombre")
    suspend fun getPorNombre(nombre: String): CoctelEntity? // <-- CAMBIADO a nulable (CoctelEntity?)

    // Función para insertar un solo cóctel (útil para el formulario de admin)
    // Devuelve el nuevo ID del cóctel insertado.
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Puedes elegir ABORT, IGNORE, o REPLACE
    suspend fun insertCoctel(coctel: CoctelEntity): Long

    // Tu función existente para insertar múltiples, también puedes añadirle una estrategia de conflicto
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ejemplo
    suspend fun insertAll(vararg cocteles: CoctelEntity)

    // Función para actualizar un cóctel existente
    @Update
    suspend fun updateCoctel(coctel: CoctelEntity)

    // Función para borrar un cóctel (necesaria para la funcionalidad de admin en CartaActivity)
    @Delete
    suspend fun deleteCoctel(coctel: CoctelEntity)
}