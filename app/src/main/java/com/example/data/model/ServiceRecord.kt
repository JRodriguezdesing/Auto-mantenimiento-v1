package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"]), Index(value = ["clientId"])]
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val clientId: Long,
    val serviceDate: Long = System.currentTimeMillis(),
    val mileageAtService: Int,
    val serviceType: String, // Cambio de Aceite y Filtro, Frenos, Afinamiento, etc.
    val oilDetails: String = "", // ej. "5W-30 Sintético Castrol Edge"
    val partsDescription: String = "",
    val partsCost: Double = 0.0,
    val laborCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val nextServiceMileage: Int = 0, // ej. kilometraje para próximo servicio
    val nextServiceDate: Long = 0L,  // fecha calculada de próximo servicio
    val notes: String = "",
    val isSyncedToSheets: Boolean = false
)
