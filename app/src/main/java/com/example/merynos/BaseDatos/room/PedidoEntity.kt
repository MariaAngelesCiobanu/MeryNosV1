package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "Pedidos")
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id_pedido: Int = 0,
    val id_mesa: Int,
    val estado: String = "pendiente",
    val id_usuario: Int,  // ← Añadir este campo si quieres vincular al usuario
    val fechaHora: Long = System.currentTimeMillis()
)