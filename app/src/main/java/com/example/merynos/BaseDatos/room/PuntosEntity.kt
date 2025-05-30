package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "Puntos")
data class PuntosEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_usuario: Int,
    val puntos: Int,
    val puntosTotales: Int


)

