package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vehicles",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"]), Index(value = ["plateNumber"])]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientId: Long,
    val plateNumber: String,
    val brand: String,
    val model: String,
    val year: Int = 2020,
    val currentMileage: Int = 0,
    val color: String = "",
    val engineType: String = "Gasolina", // Gasolina, Diésel, Híbrido, Eléctrico, GLP/GNV
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
