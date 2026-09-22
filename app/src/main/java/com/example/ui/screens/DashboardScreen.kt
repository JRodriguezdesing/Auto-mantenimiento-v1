package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertStatus
import com.example.data.model.MaintenanceAlert
import com.example.data.model.ServiceRecord
import com.example.ui.DashboardStats
import com.example.ui.theme.AutoAmber
import com.example.ui.theme.AutoBlue
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.AutoRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    stats: DashboardStats,
    alerts: List<MaintenanceAlert>,
    allServices: List<ServiceRecord> = emptyList(),
    onSendWhatsApp: (MaintenanceAlert) -> Unit,
    onSendEmail: (MaintenanceAlert) -> Unit,
    onDispatchPush: (MaintenanceAlert) -> Unit,
    onTestNotification: () -> Unit,
    onQuickSync: () -> Unit,
    onRegisterServiceForVehicle: (Long, Long) -> Unit,
    onAddNewService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("TODOS") }
    var selectedAlertForDetail by remember { mutableStateOf<MaintenanceAlert?>(null) }
    var alertForQuoteDialog by remember { mutableStateOf<MaintenanceAlert?>(null) }

    val filteredAlerts = remember(alerts, selectedFilter) {
        when (selectedFilter) {
            "VENCIDOS" -> alerts.filter { it.status == AlertStatus.OVERDUE }
            "PRÓXIMOS" -> alerts.filter { it.status == AlertStatus.UPCOMING }
            "AL DÍA" -> alerts.filter { it.status == AlertStatus.UP_TO_DATE }
            else -> alerts
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workshop Hero Header
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "AutoMantenimiento Pro",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Control Técnico & Recordatorios Centralizados",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onQuickSync,
                                modifier = Modifier.testTag("quick_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Respaldo Rápido a Sheets",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Explicit Prominent Button to Register New Maintenance
                        Button(
                            onClick = onAddNewService,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("hero_add_maintenance_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrar Nuevo Mantenimiento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onTestNotification,
                                modifier = Modifier.weight(1f).testTag("test_push_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probar Push", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onQuickSync,
                                modifier = Modifier.weight(1f).testTag("sync_sheets_btn"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Respaldo Sheets", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Clientes",
                        value = "${stats.totalClients}",
                        icon = Icons.Default.People,
                        color = AutoBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Vehículos",
                        value = "${stats.totalVehicles}",
                        icon = Icons.Default.DirectionsCar,
                        color = AutoGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Servicios",
                        value = "${stats.totalServices}",
                        icon = Icons.Default.Build,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Vencidos",
                        value = "${stats.overdueAlerts}",
                        icon = Icons.Default.Warning,
                        color = if (stats.overdueAlerts > 0) AutoRed else AutoGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section Title & Filters
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Alertas de Mantenimiento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${filteredAlerts.size} autos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TODOS", "VENCIDOS", "PRÓXIMOS", "AL DÍA").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_chip_$filter")
                        )
                    }
                }
            }
        }

        // Alert Items
        if (filteredAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AutoGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay vehículos en este filtro",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredAlerts, key = { "${it.vehicle.id}_${it.serviceRecordId}" }) { alert ->
                MaintenanceAlertCard(
                    alert = alert,
                    onClickCard = { selectedAlertForDetail = alert },
                    onOpenQuote = { alertForQuoteDialog = alert },
                    onSendWhatsApp = { onSendWhatsApp(alert) },
                    onSendEmail = { onSendEmail(alert) },
                    onDispatchPush = { onDispatchPush(alert) },
                    onRegisterService = {
                        onRegisterServiceForVehicle(alert.client.id, alert.vehicle.id)
                    }
                )
            }
        }
    }

    // Detail Dialog for Screenshot / PDF / Full View
    selectedAlertForDetail?.let { alert ->
        val matchingService = allServices.find { it.id == alert.serviceRecordId }
            ?: allServices.filter { it.vehicleId == alert.vehicle.id }.maxByOrNull { it.serviceDate }
        VehicleRecordDetailDialog(
            client = alert.client,
            vehicle = alert.vehicle,
            service = matchingService,
            alert = alert,
            onDismiss = { selectedAlertForDetail = null },
            onEditService = { v ->
                selectedAlertForDetail = null
                onRegisterServiceForVehicle(alert.client.id, v.id)
            },
            onOpenQuote = { c, v, s ->
                selectedAlertForDetail = null
                alertForQuoteDialog = alert
            }
        )
    }

    // Direct Materials & Labor Quote Dialog
    alertForQuoteDialog?.let { alert ->
        val matchingService = allServices.find { it.id == alert.serviceRecordId }
            ?: allServices.filter { it.vehicleId == alert.vehicle.id }.maxByOrNull { it.serviceDate }
        QuoteDialog(
            client = alert.client,
            vehicle = alert.vehicle,
            service = matchingService,
            onDismiss = { alertForQuoteDialog = null }
        )
    }

    ExtendedFloatingActionButton(
        onClick = onAddNewService,
        icon = { Icon(Icons.Default.Build, contentDescription = null) },
        text = { Text("Nuevo Mantenimiento", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .testTag("dashboard_add_maintenance_fab")
    )
}
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MaintenanceAlertCard(
    alert: MaintenanceAlert,
    onClickCard: () -> Unit,
    onOpenQuote: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendEmail: () -> Unit,
    onDispatchPush: () -> Unit,
    onRegisterService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val targetDateStr = if (alert.targetDate > 0L) dateFormat.format(Date(alert.targetDate)) else "Sin fecha"

    val (badgeText, badgeBg, badgeTextColor) = when (alert.status) {
        AlertStatus.OVERDUE -> Triple("VENCIDO", AutoRed.copy(alpha = 0.15f), AutoRed)
        AlertStatus.UPCOMING -> Triple("PRÓXIMO", AutoAmber.copy(alpha = 0.2f), AutoAmber)
        AlertStatus.UP_TO_DATE -> Triple("AL DÍA", AutoGreen.copy(alpha = 0.15f), AutoGreen)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClickCard() }
            .testTag("alert_card_${alert.vehicle.plateNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Plate + Brand/Model + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Plate Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = alert.vehicle.plateNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${alert.vehicle.brand} ${alert.vehicle.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cliente: ${alert.client.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Service Info & Mileage Comparison
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔧 ${alert.serviceType}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Actual: ${alert.currentMileage} km",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (alert.targetMileage > 0) {
                            Text(
                                text = "Meta: ${alert.targetMileage} km",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (alert.kmRemaining < 0) AutoRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (alert.targetDate > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📅 Programado para: $targetDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Centralized Notification & Communication Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // WhatsApp Button
                Button(
                    onClick = onSendWhatsApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AutoGreen
                    ),
                    modifier = Modifier.weight(1f).testTag("whatsapp_btn_${alert.vehicle.plateNumber}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Email Button
                FilledTonalButton(
                    onClick = onSendEmail,
                    modifier = Modifier.weight(0.9f).testTag("email_btn_${alert.vehicle.plateNumber}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Correo", fontSize = 12.sp)
                }

                // Push Notification Button
                IconButton(
                    onClick = onDispatchPush,
                    modifier = Modifier.testTag("push_btn_${alert.vehicle.plateNumber}")
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Enviar Push",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Quote Button (Cotizar Repuestos y Mano de Obra)
                IconButton(
                    onClick = onOpenQuote,
                    modifier = Modifier.testTag("quote_shortcut_${alert.vehicle.plateNumber}")
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Cotizar Repuestos y Mano de Obra",
                        tint = Color(0xFF0284C7)
                    )
                }

                // Service Shortcut (Llave para registrar / editar mantenimiento)
                IconButton(
                    onClick = onRegisterService,
                    modifier = Modifier.testTag("service_shortcut_${alert.vehicle.plateNumber}")
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Registrar / Editar Servicio",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "👆 Toca la tarjeta para ver ficha técnica detallada y compartir",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
