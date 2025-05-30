package com.example.merynos.BaseDatos

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.merynos.room.*

@Database(
    entities = [
        UsuarioEntity::class,
        MesaEntity::class,
        CoctelEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class,
        PuntosEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun mesaDao(): MesaDao
    abstract fun coctelDao(): CoctelDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun puntosDao(): PuntosDao
}