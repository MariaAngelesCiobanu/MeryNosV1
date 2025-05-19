
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

    @Query("UPDATE Pedidos SET estado = :nuevoEstado WHERE id_pedido = :idPedido")
    suspend fun actualizarEstado(idPedido: Int, nuevoEstado: String)

    @Query("SELECT * FROM DetallePedido WHERE id_pedido = :pedidoId")
    suspend fun obtenerDetallesPedido(pedidoId: Int): List<DetallePedidoEntity>

    @Query("""
    UPDATE DetallePedido 
    SET cantidad = :nuevaCantidad 
    WHERE id_pedido = :pedidoId AND id_coctel = :coctelId
""")
    suspend fun actualizarCantidad(pedidoId: Int, coctelId: Int, nuevaCantidad: Int)

    @Query("SELECT * FROM Pedidos WHERE id_mesa = :idMesa AND estado != 'pendiente'")
    suspend fun obtenerHistorialPorMesa(idMesa: Int): List<PedidoEntity>

    @Query("SELECT * FROM Pedidos WHERE id_usuario = :idUsuario AND estado != 'pendiente'")
    suspend fun obtenerHistorialPorUsuario(idUsuario: Int): List<PedidoEntity>


}
