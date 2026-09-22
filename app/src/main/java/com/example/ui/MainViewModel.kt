package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AlertStatus
import com.example.data.model.Client
import com.example.data.model.MaintenanceAlert
import com.example.data.model.ServiceRecord
import com.example.data.model.ServiceWithDetails
import com.example.data.model.Vehicle
import com.example.data.model.VehicleWithClient
import com.example.data.repository.AutoRepository
import com.example.service.GoogleSheetsSyncService
import com.example.service.NotificationHelper
import com.example.service.ReminderMessagingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardStats(
    val totalClients: Int = 0,
    val totalVehicles: Int = 0,
    val totalServices: Int = 0,
    val overdueAlerts: Int = 0,
    val upcomingAlerts: Int = 0,
    val monthlyRevenue: Double = 0.0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AutoRepository(db.clientDao(), db.vehicleDao(), db.serviceRecordDao())
    val syncService = GoogleSheetsSyncService(application)

    // Data streams from repository
    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vehiclesWithClients: StateFlow<List<VehicleWithClient>> = repository.vehiclesWithClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val servicesWithDetails: StateFlow<List<ServiceWithDetails>> = repository.servicesWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<MaintenanceAlert>> = repository.maintenanceAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search filters
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery = _clientSearchQuery.asStateFlow()

    private val _serviceSearchQuery = MutableStateFlow("")
    val serviceSearchQuery = _serviceSearchQuery.asStateFlow()

    val filteredClients: StateFlow<List<Client>> =
        combine(clients, _clientSearchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query) ||
                it.documentId.contains(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredServices: StateFlow<List<ServiceWithDetails>> =
        combine(servicesWithDetails, _serviceSearchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.client.name.contains(query, ignoreCase = true) ||
                it.vehicle.plateNumber.contains(query, ignoreCase = true) ||
                it.vehicle.brand.contains(query, ignoreCase = true) ||
                it.vehicle.model.contains(query, ignoreCase = true) ||
                it.service.serviceType.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val stats: StateFlow<DashboardStats> =
        combine(clients, vehicles, servicesWithDetails, alerts) { cList, vList, sList, aList ->
            val overdue = aList.count { it.status == AlertStatus.OVERDUE }
            val upcoming = aList.count { it.status == AlertStatus.UPCOMING }

            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val startOfMonth = cal.timeInMillis
            val revenueThisMonth = sList
                .filter { it.service.serviceDate >= startOfMonth }
                .sumOf { it.service.totalCost }

            DashboardStats(
                totalClients = cList.size,
                totalVehicles = vList.size,
                totalServices = sList.size,
                overdueAlerts = overdue,
                upcomingAlerts = upcoming,
                monthlyRevenue = revenueThisMonth
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun setClientSearchQuery(q: String) {
        _clientSearchQuery.value = q
    }

    fun setServiceSearchQuery(q: String) {
        _serviceSearchQuery.value = q
    }

    // Client CRUD
    fun saveClient(client: Client, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (client.id == 0L) {
                val newId = repository.insertClient(client)
                onDone(newId)
            } else {
                repository.updateClient(client)
                onDone(client.id)
            }
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // Vehicle CRUD
    fun saveVehicle(vehicle: Vehicle, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (vehicle.id == 0L) {
                repository.insertVehicle(vehicle)
            } else {
                repository.updateVehicle(vehicle)
            }
            onDone()
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    // Service CRUD
    fun saveService(service: ServiceRecord, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (service.id == 0L) {
                repository.insertService(service)
            } else {
                repository.updateService(service)
            }
            onDone()
        }
    }

    fun deleteService(service: ServiceRecord) {
        viewModelScope.launch {
            repository.deleteService(service)
        }
    }

    // Communication Actions
    fun sendWhatsAppReminder(alert: MaintenanceAlert) {
        ReminderMessagingHelper.sendViaWhatsApp(getApplication(), alert)
    }

    fun sendEmailReminder(alert: MaintenanceAlert) {
        ReminderMessagingHelper.sendViaEmail(getApplication(), alert)
    }

    fun dispatchPushNotificationsForUrgentAlerts() {
        val currentAlerts = alerts.value
        val urgent = currentAlerts.filter { it.status == AlertStatus.OVERDUE || it.status == AlertStatus.UPCOMING }
        if (urgent.isEmpty()) {
            NotificationHelper.sendTestNotification(getApplication())
        } else {
            urgent.take(3).forEachIndexed { index, alert ->
                NotificationHelper.sendMaintenanceAlertNotification(getApplication(), alert, 100 + index)
            }
        }
    }

    // Backup & Sync
    fun syncWithGoogleSheets() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Iniciando respaldo en Google Sheets..."

            val currentClients = clients.value
            val currentVehicles = vehicles.value
            val currentServices = servicesWithDetails.value.map { it.service }

            val result = syncService.syncWithWebhook(currentClients, currentVehicles, currentServices)
            result.onSuccess { msg ->
                _syncMessage.value = msg
            }.onFailure { err ->
                _syncMessage.value = "Fallo: ${err.localizedMessage}"
            }
            _isSyncing.value = false
        }
    }

    fun exportAndShareCsv() {
        val currentClients = clients.value
        val currentVehicles = vehicles.value
        val currentServices = servicesWithDetails.value.map { it.service }
        syncService.generateAndShareCsv(currentClients, currentVehicles, currentServices)
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
