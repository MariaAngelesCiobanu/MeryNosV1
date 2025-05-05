package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM Usuarios WHERE email = :email LIMIT 1")
    suspend fun getPorEmail(email: String): UsuarioEntity?

    @Insert
    suspend fun insertUsuario(usuario: UsuarioEntity): Long
}
