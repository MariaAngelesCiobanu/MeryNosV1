package com.example.merynos.room

import androidx.lifecycle.LiveData // <-- ¡Nueva importación!
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction // Necesario si usas relaciones complejas (PedidoConDetalles)

@Dao
interface PedidoDao {
    @Insert
    suspend fun insertarPedido(pedido: PedidoEntity): Long

    @Query("DELETE FROM DetallePedido WHERE id_detalle_pedido = :idDetallePedido")
    suspend fun eliminarDetallePedido(idDetallePedido: Int)
    @Insert
    suspend fun insertarDetalle(detalle: DetallePedidoEntity)

    @Query("SELECT * FROM Pedidos WHERE id_mesa = :idMesa AND estado = :estado")
    suspend fun obtenerPorMesaYEstado(idMesa: Int, estado: String): List<PedidoEntity>

    @Query("SELECT * FROM Pedidos WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: String): List<PedidoEntity>

    @Query("SELECT * FROM Pedidos WHERE id_pedido = :idPedido LIMIT 1")
    suspend fun obtenerPedidoPorId(idPedido: Int): PedidoEntity?

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

    @Query("UPDATE DetallePedido SET cantidad = :nuevaCantidad WHERE id_detalle_pedido = :idDetallePedido")
    suspend fun actualizarCantidadDetallePedido(idDetallePedido: Int, nuevaCantidad: Int)

    // --- ¡NUEVO MÉTODO CRUCIAL PARA EL BARMAN! ---
    // Este método devolverá una lista de pedidos LiveData con estado 'confirmado'
    // LiveData asegura que tu UI se actualice automáticamente cuando la base de datos cambie.
    @Query("SELECT * FROM Pedidos WHERE estado = 'confirmado' ORDER BY id_pedido ASC")
    fun obtenerPedidosConfirmadosParaBarman(): LiveData<List<PedidoEntity>>

}