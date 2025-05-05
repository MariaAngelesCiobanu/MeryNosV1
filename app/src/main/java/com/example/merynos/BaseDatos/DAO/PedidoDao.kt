
package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PedidoDao {
    @Insert
    suspend fun insertarPedido(pedido: PedidoEntity): Long

    @Insert
    suspend fun insertarDetalle(detalle: DetallePedidoEntity)

    @Query("SELECT * FROM Pedidos WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: String): List<PedidoEntity>
}
