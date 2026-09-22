package com.example.data.repository

import com.example.data.local.ClientDao
import com.example.data.local.ServiceRecordDao
import com.example.data.local.VehicleDao
import com.example.data.model.AlertStatus
import com.example.data.model.Client
import com.example.data.model.MaintenanceAlert
import com.example.data.model.ServiceRecord
import com.example.data.model.ServiceWithDetails
import com.example.data.model.Vehicle
import com.example.data.model.VehicleWithClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class AutoRepository(
    private val clientDao: ClientDao,
    private val vehicleDao: VehicleDao,
    private val serviceDao: ServiceRecordDao
) {
    // Clients
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    fun searchClients(query: String): Flow<List<Client>> = clientDao.searchClients(query)
    suspend fun getClientById(id: Long): Client? = clientDao.getClientById(id)
    suspend fun insertClient(client: Client): Long = clientDao.insertClient(client)
    suspend fun updateClient(client: Client) = clientDao.updateClient(client)
    suspend fun deleteClient(client: Client) = clientDao.deleteClient(client)
    val clientCount: Flow<Int> = clientDao.getClientCount()

    // Vehicles
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    fun getVehiclesByClient(clientId: Long): Flow<List<Vehicle>> = vehicleDao.getVehiclesByClient(clientId)
    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)
    suspend fun getVehicleByPlate(plate: String): Vehicle? = vehicleDao.getVehicleByPlate(plate)
    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)
    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)
    suspend fun updateMileage(vehicleId: Long, newMileage: Int) = vehicleDao.updateMileage(vehicleId, newMileage)
    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)
    val vehicleCount: Flow<Int> = vehicleDao.getVehicleCount()

    // Services
    val allServices: Flow<List<ServiceRecord>> = serviceDao.getAllServices()
    fun getServicesByVehicle(vehicleId: Long): Flow<List<ServiceRecord>> = serviceDao.getServicesByVehicle(vehicleId)
    suspend fun insertService(service: ServiceRecord): Long {
        val id = serviceDao.insertService(service)
        // Also update vehicle mileage if service mileage is higher
        val vehicle = vehicleDao.getVehicleById(service.vehicleId)
        if (vehicle != null && service.mileageAtService > vehicle.currentMileage) {
            vehicleDao.updateMileage(vehicle.id, service.mileageAtService)
        }
        return id
    }
    suspend fun updateService(service: ServiceRecord) = serviceDao.updateService(service)
    suspend fun deleteService(service: ServiceRecord) = serviceDao.deleteService(service)
    val serviceCount: Flow<Int> = serviceDao.getServiceCount()
    fun getRevenueThisMonth(startOfMonthTimestamp: Long): Flow<Double?> = serviceDao.getTotalRevenueSince(startOfMonthTimestamp)

    // Joined Flow: Vehicles with Client
    val vehiclesWithClients: Flow<List<VehicleWithClient>> =
        combine(allVehicles, allClients) { vehicles, clients ->
            val clientMap = clients.associateBy { it.id }
            vehicles.mapNotNull { vehicle ->
                val client = clientMap[vehicle.clientId]
                if (client != null) VehicleWithClient(vehicle, client) else null
            }
        }

    // Joined Flow: Full Service History With Client & Vehicle
    val servicesWithDetails: Flow<List<ServiceWithDetails>> =
        combine(allServices, allVehicles, allClients) { services, vehicles, clients ->
            val vehicleMap = vehicles.associateBy { it.id }
            val clientMap = clients.associateBy { it.id }
            services.mapNotNull { service ->
                val vehicle = vehicleMap[service.vehicleId]
                val client = clientMap[service.clientId]
                if (vehicle != null && client != null) {
                    ServiceWithDetails(service, vehicle, client)
                } else null
            }
        }

    // Reactive Maintenance Alerts Flow:
    // Examines vehicles and their latest service records to identify overdue and upcoming maintenance
    val maintenanceAlerts: Flow<List<MaintenanceAlert>> =
        combine(allVehicles, allClients, allServices) { vehicles, clients, services ->
            val clientMap = clients.associateBy { it.id }
            val latestServiceByVehicle = services.groupBy { it.vehicleId }
                .mapValues { entry -> entry.value.maxByOrNull { it.serviceDate } }

            val currentTime = System.currentTimeMillis()
            val alerts = mutableListOf<MaintenanceAlert>()

            for (vehicle in vehicles) {
                val client = clientMap[vehicle.clientId] ?: continue
                val latestService = latestServiceByVehicle[vehicle.id]

                if (latestService != null && (latestService.nextServiceMileage > 0 || latestService.nextServiceDate > 0L)) {
                    val targetMileage = latestService.nextServiceMileage
                    val targetDate = latestService.nextServiceDate
                    val kmRemaining = targetMileage - vehicle.currentMileage
                    val msRemaining = targetDate - currentTime
                    val daysRemaining = if (targetDate > 0L) msRemaining / (1000 * 60 * 60 * 24) else 999L

                    // Determine Alert Status
                    val isMileageOverdue = targetMileage in 1..vehicle.currentMileage
                    val isDateOverdue = targetDate in 1..currentTime
                    val isMileageUpcoming = targetMileage > 0 && kmRemaining in 0..1000
                    val isDateUpcoming = targetDate > 0L && daysRemaining in 0..15

                    val status = when {
                        isMileageOverdue || isDateOverdue -> AlertStatus.OVERDUE
                        isMileageUpcoming || isDateUpcoming -> AlertStatus.UPCOMING
                        else -> AlertStatus.UP_TO_DATE
                    }

                    alerts.add(
                        MaintenanceAlert(
                            serviceRecordId = latestService.id,
                            client = client,
                            vehicle = vehicle,
                            serviceType = latestService.serviceType,
                            lastServiceDate = latestService.serviceDate,
                            lastMileage = latestService.mileageAtService,
                            targetMileage = targetMileage,
                            targetDate = targetDate,
                            currentMileage = vehicle.currentMileage,
                            status = status,
                            daysRemaining = daysRemaining,
                            kmRemaining = kmRemaining
                        )
                    )
                } else if (vehicle.currentMileage >= 5000) {
                    // Vehicle without registered service history but high mileage -> suggested initial inspection
                    alerts.add(
                        MaintenanceAlert(
                            serviceRecordId = 0,
                            client = client,
                            vehicle = vehicle,
                            serviceType = "Inspección Inicial / Cambio de Aceite",
                            lastServiceDate = vehicle.updatedAt,
                            lastMileage = 0,
                            targetMileage = vehicle.currentMileage,
                            targetDate = currentTime,
                            currentMileage = vehicle.currentMileage,
                            status = AlertStatus.UPCOMING,
                            daysRemaining = 0,
                            kmRemaining = 0
                        )
                    )
                }
            }

            // Order by urgency: OVERDUE first, then UPCOMING, then UP_TO_DATE
            alerts.sortedWith(
                compareBy<MaintenanceAlert> {
                    when (it.status) {
                        AlertStatus.OVERDUE -> 0
                        AlertStatus.UPCOMING -> 1
                        AlertStatus.UP_TO_DATE -> 2
                    }
                }.thenBy { it.daysRemaining }
            )
        }

    // Seed realistic sample data if empty so the app has high utility from first run
    suspend fun checkAndSeedInitialData() {
        val count = clientDao.getClientCount().first()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L

            // Client 1
            val c1Id = clientDao.insertClient(
                Client(
                    name = "Carlos Méndez",
                    phone = "+593991234567",
                    email = "carlos.mendez@email.com",
                    documentId = "1718293840",
                    address = "Av. América y República, Edif. Los Olivos",
                    notes = "Cliente frecuente. Prefiere aceite sintético 5W-30."
                )
            )
            val v1Id = vehicleDao.insertVehicle(
                Vehicle(
                    clientId = c1Id,
                    plateNumber = "PBX-4592",
                    brand = "Toyota",
                    model = "Hilux 4x4",
                    year = 2022,
                    currentMileage = 55400,
                    color = "Plata Metálico",
                    engineType = "Diésel 2.8 D-4D"
                )
            )
            serviceDao.insertService(
                ServiceRecord(
                    vehicleId = v1Id,
                    clientId = c1Id,
                    serviceDate = now - (100 * dayMs),
                    mileageAtService = 50000,
                    serviceType = "Cambio de Aceite y Filtro",
                    oilDetails = "Aceite 5W-30 Sintético Diésel Mobil Delvac (7.5 Qt) + Filtro Original",
                    partsDescription = "Filtro de aceite, arandela de tapón de cárter",
                    partsCost = 65.0,
                    laborCost = 20.0,
                    totalCost = 85.0,
                    nextServiceMileage = 55000, // Now overdue by 400 km!
                    nextServiceDate = now - (10 * dayMs),
                    notes = "Próximo cambio requerido a los 55,000 km."
                )
            )

            // Client 2
            val c2Id = clientDao.insertClient(
                Client(
                    name = "Sofía Paredes",
                    phone = "+593987654321",
                    email = "sofia.paredes@gmail.com",
                    documentId = "0923485712",
                    address = "Calle Los Álamos #340",
                    notes = "Avisar por WhatsApp un par de días antes."
                )
            )
            val v2Id = vehicleDao.insertVehicle(
                Vehicle(
                    clientId = c2Id,
                    plateNumber = "GSH-8120",
                    brand = "Chevrolet",
                    model = "Tracker Turbo",
                    year = 2023,
                    currentMileage = 29650,
                    color = "Rojo Rubí",
                    engineType = "Gasolina 1.2L Turbo"
                )
            )
            serviceDao.insertService(
                ServiceRecord(
                    vehicleId = v2Id,
                    clientId = c2Id,
                    serviceDate = now - (75 * dayMs),
                    mileageAtService = 25000,
                    serviceType = "Cambio de Aceite y Filtro",
                    oilDetails = "Dexos 1 Gen 3 0W-20 Full Sintético ACDelco (4.2 L)",
                    partsDescription = "Filtro aceite motor + filtro de aire de cabina",
                    partsCost = 55.0,
                    laborCost = 25.0,
                    totalCost = 80.0,
                    nextServiceMileage = 30000, // Upcoming (in 350 km!)
                    nextServiceDate = now + (15 * dayMs),
                    notes = "Revisión de pastillas de freno en el próximo servicio."
                )
            )

            // Client 3
            val c3Id = clientDao.insertClient(
                Client(
                    name = "Ing. Roberto Gómez",
                    phone = "+593954321987",
                    email = "roberto.gomez@empresa.ec",
                    documentId = "1103492817",
                    address = "Parque Industrial, Lote 12",
                    notes = "Vehículo de gerencia. Factura con datos de empresa."
                )
            )
            val v3Id = vehicleDao.insertVehicle(
                Vehicle(
                    clientId = c3Id,
                    plateNumber = "PCA-3104",
                    brand = "Nissan",
                    model = "Kicks Exclusive",
                    year = 2021,
                    currentMileage = 42100,
                    color = "Blanco Perlado",
                    engineType = "Gasolina 1.6L"
                )
            )
            serviceDao.insertService(
                ServiceRecord(
                    vehicleId = v3Id,
                    clientId = c3Id,
                    serviceDate = now - (15 * dayMs),
                    mileageAtService = 41500,
                    serviceType = "Mantenimiento General y Frenos",
                    oilDetails = "5W-30 Sintético Nissan Ester Oil",
                    partsDescription = "Juego pastillas de freno delanteras cerámicas, líquido DOT 4, filtro de aire y aceite",
                    partsCost = 110.0,
                    laborCost = 45.0,
                    totalCost = 155.0,
                    nextServiceMileage = 46500, // Up to date!
                    nextServiceDate = now + (90 * dayMs),
                    notes = "Discos rectificados en excelente estado."
                )
            )
        }
    }
}
