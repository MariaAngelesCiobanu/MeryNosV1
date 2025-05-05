package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id_usuario: Int = 0,
    val nombreYApellidos: String,
    val email: String,
    val contraseña: String,
    val rol: String = "cliente" // cliente o barman
)
