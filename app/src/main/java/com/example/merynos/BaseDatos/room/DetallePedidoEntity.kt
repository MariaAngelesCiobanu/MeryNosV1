package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DetallePedido")
data class DetallePedidoEntity(
    @PrimaryKey(autoGenerate = true)
    val id_detalle_pedido: Int = 0,
    val id_pedido: Int,
    val id_coctel: Int,
    val cantidad: Int
)