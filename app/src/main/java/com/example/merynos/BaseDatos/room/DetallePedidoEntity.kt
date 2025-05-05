
package com.example.merynos.room

import androidx.room.Entity

@Entity(primaryKeys = ["id_pedido", "id_coctel"], tableName = "DetallePedido")
data class DetallePedidoEntity(
    val id_pedido: Int,
    val id_coctel: Int,
    val cantidad: Int = 1
)
