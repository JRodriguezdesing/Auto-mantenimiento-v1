package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val documentId: String = "", // Cédula, RUT o DNI
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
