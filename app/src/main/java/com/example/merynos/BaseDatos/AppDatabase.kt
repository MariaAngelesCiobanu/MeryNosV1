
package com.example.merynos.BaseDatos

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.merynos.room.*

@Database(
    entities = [
        CoctelEntity::class,
        MesaEntity::class,
        UsuarioEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coctelDao(): CoctelDao
    abstract fun mesaDao(): MesaDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun pedidoDao(): PedidoDao
}
