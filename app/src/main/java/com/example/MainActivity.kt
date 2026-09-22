package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Client
import com.example.data.model.Vehicle
import com.example.service.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.screens.AddEditClientDialog
import com.example.ui.screens.AddEditVehicleDialog
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewServiceScreen
import com.example.ui.screens.ServiceHistoryScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

enum class AppDestination(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Inicio", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    CLIENTS("Clientes", Icons.Filled.People, Icons.Outlined.People),
    NEW_SERVICE("+ Servicio", Icons.Filled.Build, Icons.Outlined.Build),
    HISTORY("Historial", Icons.Filled.History, Icons.Outlined.History),
    BACKUP("Respaldo", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                AutoMaintenanceApp()
            }
        }
    }
}

@Composable
fun AutoMaintenanceApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation State
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }

    // Dialog States
    var showClientDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }

    var showVehicleDialog by remember { mutableStateOf(false) }
    var vehicleClientTarget by remember { mutableStateOf<Client?>(null) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }

    var preselectedVehicleIdForService by remember { mutableStateOf<Long?>(null) }

    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permisos de notificaciones concedidos", Toast.LENGTH_SHORT).show()
            NotificationHelper.sendTestNotification(context)
        } else {
            Toast.makeText(context, "Permiso denegado. Puede activarlo en Ajustes.", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-request notification permission on launch for Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Observe Data
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val filteredClients by viewModel.filteredClients.collectAsStateWithLifecycle()
    val allVehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val vehiclesWithClients by viewModel.vehiclesWithClients.collectAsStateWithLifecycle()
    val servicesWithDetails by viewModel.servicesWithDetails.collectAsStateWithLifecycle()
    val filteredServices by viewModel.filteredServices.collectAsStateWithLifecycle()
    val allServices = remember(servicesWithDetails) { servicesWithDetails.map { it.service } }
    val clientSearchQuery by viewModel.clientSearchQuery.collectAsStateWithLifecycle()
    val serviceSearchQuery by viewModel.serviceSearchQuery.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    // Sync notification message observer
    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentDestination, label = "screen_crossfade") { destination ->
                when (destination) {
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            stats = stats,
                            alerts = alerts,
                            allServices = allServices,
                            onSendWhatsApp = { alert -> viewModel.sendWhatsAppReminder(alert) },
                            onSendEmail = { alert -> viewModel.sendEmailReminder(alert) },
                            onDispatchPush = { alert ->
                                NotificationHelper.sendMaintenanceAlertNotification(context, alert, alert.vehicle.id.toInt())
                                Toast.makeText(context, "Notificación push enviada", Toast.LENGTH_SHORT).show()
                            },
                            onTestNotification = {
                                viewModel.dispatchPushNotificationsForUrgentAlerts()
                                Toast.makeText(context, "Notificaciones del sistema generadas", Toast.LENGTH_SHORT).show()
                            },
                            onQuickSync = { viewModel.syncWithGoogleSheets() },
                            onRegisterServiceForVehicle = { _, vehicleId ->
                                preselectedVehicleIdForService = vehicleId
                                currentDestination = AppDestination.NEW_SERVICE
                            },
                            onAddNewService = {
                                preselectedVehicleIdForService = null
                                currentDestination = AppDestination.NEW_SERVICE
                            }
                        )
                    }

                    AppDestination.CLIENTS -> {
                        ClientsScreen(
                            clients = filteredClients,
                            vehicles = allVehicles,
                            searchQuery = clientSearchQuery,
                            onSearchChange = { viewModel.setClientSearchQuery(it) },
                            onAddClientClick = {
                                editingClient = null
                                showClientDialog = true
                            },
                            onEditClientClick = { client ->
                                editingClient = client
                                showClientDialog = true
                            },
                            onDeleteClientClick = { client ->
                                viewModel.deleteClient(client)
                            },
                            onAddVehicleClick = { client ->
                                vehicleClientTarget = client
                                editingVehicle = null
                                showVehicleDialog = true
                            },
                            onEditVehicleClick = { vehicle, client ->
                                vehicleClientTarget = client
                                editingVehicle = vehicle
                                showVehicleDialog = true
                            },
                            onDeleteVehicleClick = { vehicle ->
                                viewModel.deleteVehicle(vehicle)
                            },
                            onRegisterServiceForVehicle = { _, vehicleId ->
                                preselectedVehicleIdForService = vehicleId
                                currentDestination = AppDestination.NEW_SERVICE
                            }
                        )
                    }

                    AppDestination.NEW_SERVICE -> {
                        NewServiceScreen(
                            vehiclesWithClients = vehiclesWithClients,
                            preselectedVehicleId = preselectedVehicleIdForService,
                            existingServices = allServices,
                            onSaveService = { service ->
                                viewModel.saveService(service)
                            },
                            onServiceSavedSuccess = {
                                preselectedVehicleIdForService = null
                                currentDestination = AppDestination.HISTORY
                            }
                        )
                    }

                    AppDestination.HISTORY -> {
                        ServiceHistoryScreen(
                            services = filteredServices,
                            searchQuery = serviceSearchQuery,
                            onSearchChange = { viewModel.setServiceSearchQuery(it) },
                            onDeleteService = { service -> viewModel.deleteService(service) },
                            onAddNewService = {
                                preselectedVehicleIdForService = null
                                currentDestination = AppDestination.NEW_SERVICE
                            }
                        )
                    }

                    AppDestination.BACKUP -> {
                        BackupSettingsScreen(
                            syncService = viewModel.syncService,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            onSyncNow = { viewModel.syncWithGoogleSheets() },
                            onExportCsv = { viewModel.exportAndShareCsv() },
                            onTestNotification = {
                                NotificationHelper.sendTestNotification(context)
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Client Dialog
    if (showClientDialog) {
        AddEditClientDialog(
            client = editingClient,
            onDismiss = {
                showClientDialog = false
                editingClient = null
            },
            onSave = { client ->
                viewModel.saveClient(client)
                showClientDialog = false
                editingClient = null
            }
        )
    }

    // Add / Edit Vehicle Dialog
    if (showVehicleDialog && vehicleClientTarget != null) {
        AddEditVehicleDialog(
            clientId = vehicleClientTarget!!.id,
            clientName = vehicleClientTarget!!.name,
            vehicle = editingVehicle,
            onDismiss = {
                showVehicleDialog = false
                editingVehicle = null
                vehicleClientTarget = null
            },
            onSave = { vehicle ->
                viewModel.saveVehicle(vehicle)
                showVehicleDialog = false
                editingVehicle = null
                vehicleClientTarget = null
            }
        )
    }
}

/**
 * Kept for test and screenshot compatibility
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
