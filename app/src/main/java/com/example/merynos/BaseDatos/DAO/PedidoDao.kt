package com.example.merynos.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy // Mantengo el import por si lo usas en el futuro
import androidx.room.Query
import com.example.merynos.room.DetallePedidoEntity // Asegúrate de que este import es correcto
import com.example.merynos.room.PedidoEntity      // Asegúrate de que este import es correcto


@Dao
interface PedidoDao {
    @Insert
    suspend fun insertarPedido(pedido: PedidoEntity): Long

    @Insert
    suspend fun insertarDetalle(detalle: DetallePedidoEntity)

    @Query("SELECT * FROM Pedidos WHERE id_mesa = :idMesa AND estado = :estado")
    suspend fun obtenerPorMesaYEstado(idMesa: Int, estado: String): List<PedidoEntity>

    @Query("SELECT * FROM Pedidos WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: String): List<PedidoEntity>

    // --- FUNCIÓN AÑADIDA ---
    @Query("SELECT * FROM Pedidos WHERE id_pedido = :idPedido LIMIT 1")
    suspend fun obtenerPedidoPorId(idPedido: Int): PedidoEntity? // Devuelve nulable
    // ------------------------

    @Query("UPDATE Pedidos SET estado = :nuevoEstado WHERE id_pedido = :idPedido")
    suspend fun actualizarEstado(idPedido: Int, nuevoEstado: String)

    // Usando "DetallePedido" como me lo pasaste
    @Query("SELECT * FROM DetallePedido WHERE id_pedido = :pedidoId")
    suspend fun obtenerDetallesPedido(pedidoId: Int): List<DetallePedidoEntity>

    // Usando "DetallePedido" como me lo pasaste
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