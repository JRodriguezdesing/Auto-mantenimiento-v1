package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceRecord
import com.example.data.model.ServiceWithDetails
import com.example.ui.theme.AutoAmber
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.AutoRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServiceHistoryScreen(
    services: List<ServiceWithDetails>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDeleteService: (ServiceRecord) -> Unit,
    onAddNewService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedServiceDetail by remember { mutableStateOf<ServiceWithDetails?>(null) }
    var serviceForQuoteDialog by remember { mutableStateOf<ServiceWithDetails?>(null) }
    var serviceToDelete by remember { mutableStateOf<ServiceRecord?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("service_history_screen")
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar servicio por placa, cliente o tipo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_services_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${services.size} Órdenes de Servicio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalRevenue = services.sumOf { it.service.totalCost }
                Text(
                    text = "Total: $ ${String.format(Locale.US, "%.2f", totalRevenue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (services.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No hay servicios registrados todavía" else "No se encontraron coincidencias para '$searchQuery'",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onAddNewService,
                                modifier = Modifier.testTag("empty_add_maintenance_btn")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Registrar Primer Mantenimiento")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(services, key = { it.service.id }) { item ->
                        ServiceHistoryCard(
                            item = item,
                            onClick = { selectedServiceDetail = item },
                            onDelete = { serviceToDelete = item.service },
                            onShare = {
                                shareServiceTicket(context, item)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button for New Maintenance
        ExtendedFloatingActionButton(
            onClick = onAddNewService,
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            text = { Text("Nuevo Mantenimiento", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("history_add_maintenance_fab")
        )
    }

    // Full Visual Detail Dialog (Ready for Screenshot, PDF, WhatsApp, and Quote)
    selectedServiceDetail?.let { item ->
        VehicleRecordDetailDialog(
            client = item.client,
            vehicle = item.vehicle,
            service = item.service,
            alert = null,
            onDismiss = { selectedServiceDetail = null },
            onEditService = { v ->
                selectedServiceDetail = null
                onAddNewService()
            },
            onOpenQuote = { c, v, s ->
                selectedServiceDetail = null
                serviceForQuoteDialog = item
            }
        )
    }

    // Material and Labor Quote Dialog
    serviceForQuoteDialog?.let { item ->
        QuoteDialog(
            client = item.client,
            vehicle = item.vehicle,
            service = item.service,
            onDismiss = { serviceForQuoteDialog = null }
        )
    }

    // Delete Confirmation
    serviceToDelete?.let { service ->
        AlertDialog(
            onDismissRequest = { serviceToDelete = null },
            title = { Text("¿Eliminar Registro?") },
            text = { Text("Se eliminará permanentemente la orden de servicio #${service.id}.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteService(service)
                        serviceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ServiceHistoryCard(
    item: ServiceWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("service_card_${item.service.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = item.vehicle.plateNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${item.vehicle.brand} ${item.vehicle.model}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = dateFormat.format(Date(item.service.serviceDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🔧 ${item.service.serviceType}",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cliente: ${item.client.name} • ${item.service.mileageAtService} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$ ${String.format(Locale.US, "%.2f", item.service.totalCost)}",
                    fontWeight = FontWeight.Bold,
                    color = AutoGreen,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (item.service.nextServiceMileage > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Próximo servicio: a los ${item.service.nextServiceMileage} km",
                    fontSize = 11.sp,
                    color = AutoAmber,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AutoRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun shareServiceTicket(context: android.content.Context, item: ServiceWithDetails) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val text = buildString {
        append("🚗 *COMPROBANTE DE SERVICIO TÉCNICO*\n")
        append("────────────────────────\n")
        append("• *Orden:* #${item.service.id}\n")
        append("• *Fecha:* ${dateFormat.format(Date(item.service.serviceDate))}\n")
        append("• *Cliente:* ${item.client.name}\n")
        append("• *Vehículo:* ${item.vehicle.brand} ${item.vehicle.model} (${item.vehicle.year})\n")
        append("• *Placa:* ${item.vehicle.plateNumber}\n")
        append("• *Kilometraje:* ${item.service.mileageAtService} km\n")
        append("────────────────────────\n")
        append("• *Servicio:* ${item.service.serviceType}\n")
        if (item.service.oilDetails.isNotBlank()) append("• *Aceite:* ${item.service.oilDetails}\n")
        if (item.service.partsDescription.isNotBlank()) append("• *Repuestos:* ${item.service.partsDescription}\n")
        append("• *Total Cobrado:* $ ${String.format(Locale.US, "%.2f", item.service.totalCost)}\n")
        append("────────────────────────\n")
        if (item.service.nextServiceMileage > 0) {
            append("🔔 *Próximo Servicio Sugerido:* ${item.service.nextServiceMileage} km\n")
        }
        append("\n¡Gracias por confiar en nuestro taller!")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Comprobante Servicio - ${item.vehicle.plateNumber}")
        putExtra(Intent.EXTRA_TEXT, text)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(Intent.createChooser(intent, "Compartir Comprobante"))
}
