package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DetallePedido", /*...foreign keys si las tienes...*/)
data class DetallePedidoEntity(
    @PrimaryKey(autoGenerate = true)
    val id_detalle_pedido: Int = 0, // MUY BIEN - Ya tienes la clave primaria
    val id_pedido: Int,
    val id_coctel: Int, // Actualmente no es nulable, lo cual está bien si un detalle SIEMPRE tiene un cóctel
    val cantidad: Int
)