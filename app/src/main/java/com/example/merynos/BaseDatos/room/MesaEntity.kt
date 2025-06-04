package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Mesa")
data class MesaEntity(
    @PrimaryKey(autoGenerate = true) val id_mesa: Int = 0,
    val codigoQR: String,
    val nombreMesa: String,
    var estado: String = "libre" // libre, ocupada, reservada
)
