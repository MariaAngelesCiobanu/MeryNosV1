package com.example.merynos.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Mesa")
data class MesaEntity(
    // id_mesa NO es autogenerado. Su valor DEBE ser proporcionado al crear la entidad,
    // y ese valor es el codigoQR convertido a Int.
    @PrimaryKey val id_mesa: Int, // <-- ¡Sin autoGenerate = true ni = 0!
    val codigoQR: String, // Este String debe ser un número que coincidirá con id_mesa
    var nombreMesa: String, // 'var' para que se pueda modificar
    var estado: String = "libre" // 'var' para que se pueda modificar (libre, ocupada, reservada)
)