package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Puntos")
data class PuntosEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_usuario: Int,
    val puntos: Int, // Representa el CAMBIO de puntos en esta transacción
    val puntosTotales: Int // Representa el SALDO TOTAL después de esta transacción
)