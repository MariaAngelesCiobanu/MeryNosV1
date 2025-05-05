package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "Pedidos")
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id_pedido: Int = 0,
    val id_mesa: Int,
    val estado: String = "pendiente",
    val fechaHora: Long = System.currentTimeMillis()
)