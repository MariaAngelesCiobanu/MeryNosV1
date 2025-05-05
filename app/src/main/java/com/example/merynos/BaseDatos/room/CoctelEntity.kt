// archivo: com.example.merynos/CoctelEntity.kt
package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Cocteles")
data class CoctelEntity(
    @PrimaryKey(autoGenerate = true) val id_coctel: Int = 0,
    val nombreCoctel: String,
    val historia: String,
    val metodoElaboracion: String,
    val precioCoctel: Double
)
