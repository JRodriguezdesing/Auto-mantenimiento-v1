package com.example.data.model

enum class AlertStatus {
    OVERDUE,    // Vencido (Rojo)
    UPCOMING,   // Próximo (Amarillo/Ámbar)
    UP_TO_DATE  // Al día (Verde)
}

data class MaintenanceAlert(
    val serviceRecordId: Long,
    val client: Client,
    val vehicle: Vehicle,
    val serviceType: String,
    val lastServiceDate: Long,
    val lastMileage: Int,
    val targetMileage: Int,
    val targetDate: Long,
    val currentMileage: Int,
    val status: AlertStatus,
    val daysRemaining: Long,
    val kmRemaining: Int
)

data class VehicleWithClient(
    val vehicle: Vehicle,
    val client: Client
)

data class ServiceWithDetails(
    val service: ServiceRecord,
    val vehicle: Vehicle,
    val client: Client
)
