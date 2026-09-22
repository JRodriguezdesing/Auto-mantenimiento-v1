package com.example.data.model

import java.io.Serializable

data class QuoteMaterialItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var quantity: Double = 1.0,
    var unitPrice: Double = 0.0
) : Serializable {
    val subtotal: Double
        get() = quantity * unitPrice
}

data class MaintenanceQuote(
    var quoteNumber: String = "",
    var date: Long = System.currentTimeMillis(),
    var clientName: String = "",
    var clientPhone: String = "",
    var vehiclePlate: String = "",
    var vehicleModel: String = "",
    var currentMileage: Int = 0,
    var materials: MutableList<QuoteMaterialItem> = mutableListOf(),
    var laborRole: String = "Mecánico General", // "Mecánico General", "Electromecánico", "Técnico Especialista"
    var laborDescription: String = "Mano de obra técnica y diagnóstico",
    var laborCost: Double = 0.0,
    var validityDays: Int = 15,
    var notes: String = "Precios sujetos a disponibilidad de inventario. Repuestos con garantía técnica."
) : Serializable {
    val materialsTotal: Double
        get() = materials.sumOf { it.subtotal }

    val grandTotal: Double
        get() = materialsTotal + laborCost
}
