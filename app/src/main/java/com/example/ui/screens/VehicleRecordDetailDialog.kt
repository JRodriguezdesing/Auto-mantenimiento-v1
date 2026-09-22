package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AlertStatus
import com.example.data.model.Client
import com.example.data.model.MaintenanceAlert
import com.example.data.model.ServiceRecord
import com.example.data.model.Vehicle
import com.example.service.PdfReportService
import com.example.ui.theme.AutoAmber
import com.example.ui.theme.AutoBlue
import com.example.ui.theme.AutoGreen
import com.example.ui.theme.AutoRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleRecordDetailDialog(
    client: Client,
    vehicle: Vehicle,
    service: ServiceRecord?,
    alert: MaintenanceAlert? = null,
    onDismiss: () -> Unit,
    onEditService: (Vehicle) -> Unit,
    onOpenQuote: (Client, Vehicle, ServiceRecord?) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val status = alert?.status ?: when {
        service == null -> AlertStatus.UP_TO_DATE
        service.nextServiceMileage > 0 && vehicle.currentMileage >= service.nextServiceMileage -> AlertStatus.OVERDUE
        service.nextServiceMileage > 0 && (service.nextServiceMileage - vehicle.currentMileage) <= 1000 -> AlertStatus.UPCOMING
        else -> AlertStatus.UP_TO_DATE
    }

    val (badgeText, badgeBg, badgeTextColor) = when (status) {
        AlertStatus.OVERDUE -> Triple("🔴 MANTENIMIENTO VENCIDO", AutoRed.copy(alpha = 0.15f), AutoRed)
        AlertStatus.UPCOMING -> Triple("🟡 PRÓXIMO A VENCER", AutoAmber.copy(alpha = 0.2f), AutoAmber)
        AlertStatus.UP_TO_DATE -> Triple("🟢 MANTENIMIENTO AL DÍA", AutoGreen.copy(alpha = 0.15f), AutoGreen)
    }

    fun shareRecordViaWhatsApp() {
        val cleanPhone = client.phone.replace(Regex("[^0-9+]"), "")
        val sb = StringBuilder()
        sb.append("🚗 *AUTO MANTENIMIENTO - REPORTE DE MANTENIMIENTO*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📋 *Vehículo:* ${vehicle.plateNumber} • ${vehicle.brand} ${vehicle.model} (${vehicle.year})\n")
        sb.append("👤 *Cliente:* ${client.name}\n")
        sb.append("📍 *Kilometraje Actual:* ${vehicle.currentMileage} km\n")
        sb.append("🏷️ *Estado:* $badgeText\n\n")

        if (service != null) {
            sb.append("🔧 *ÚLTIMO TRABAJO REGISTRADO:*\n")
            sb.append("• *Servicio:* ${service.serviceType}\n")
            sb.append("• *Fecha:* ${dateFormat.format(Date(service.serviceDate))}\n")
            sb.append("• *Km Servicio:* ${service.mileageAtService} km\n")
            if (service.oilDetails.isNotBlank()) sb.append("• *Aceite / Fluidos:* ${service.oilDetails}\n")
            if (service.partsDescription.isNotBlank()) sb.append("• *Repuestos:* ${service.partsDescription}\n")
            sb.append("• *Total Facturado:* $${String.format(Locale.US, "%.2f", service.totalCost)}\n\n")

            sb.append("🎯 *PRÓXIMA REVISIÓN TÉCNICA:*\n")
            if (service.nextServiceMileage > 0) sb.append("• Meta Km: *${service.nextServiceMileage} km*\n")
            if (service.nextServiceDate > 0) sb.append("• Fecha Sugerida: *${dateOnlyFormat.format(Date(service.nextServiceDate))}*\n")
            if (service.notes.isNotBlank()) sb.append("\n📝 *Observación:* ${service.notes}\n")
        } else {
            sb.append("ℹ️ *Aún no cuenta con servicios registrados en el taller.*\n")
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Gracias por confiar en *AUTO MANTENIMIENTO*.")

        val text = sb.toString()
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = if (cleanPhone.isNotBlank()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Ficha").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("vehicle_record_detail_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Workshop Blue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E3A8A))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUTO MANTENIMIENTO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ficha Técnica & Registro del Vehículo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                // Scrollable Content - Structured for Screenshot Capture
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vehicle & Status Header Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Plate Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        modifier = Modifier.border(2.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = vehicle.plateNumber,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            letterSpacing = 2.sp,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = badgeBg
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "${vehicle.brand} ${vehicle.model} (${vehicle.year})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Km Actual: ${vehicle.currentMileage} km",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Text(
                                        text = "• Combustible: ${vehicle.engineType}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Client Info Card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = AutoBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Datos del Propietario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Nombre: ${client.name}", fontWeight = FontWeight.Medium)
                                Text("Teléfono / WhatsApp: ${client.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (client.documentId.isNotBlank()) {
                                    Text("Identificación: ${client.documentId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (client.email.isNotBlank()) {
                                    Text("Correo: ${client.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Last Service Record Card
                    item {
                        if (service != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Historial: Orden #${service.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        }
                                        Text(
                                            text = dateFormat.format(Date(service.serviceDate)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Works performed
                                    Text("Trabajos Técnicos Realizados:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    val works = service.serviceType.split("+").map { it.trim() }
                                    works.forEach { w ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text("✔ ", color = AutoBlue, fontWeight = FontWeight.Bold)
                                            Text(w, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Oil details
                                    if (service.oilDetails.isNotBlank()) {
                                        Text("Lubricación & Aceite:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(service.oilDetails, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Parts details
                                    if (service.partsDescription.isNotBlank()) {
                                        Text("Repuestos Reemplazados:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(service.partsDescription, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Financial breakdown
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Repuestos: $ ${String.format(Locale.US, "%.2f", service.partsCost)}", fontSize = 11.sp)
                                            Text("Mano de Obra: $ ${String.format(Locale.US, "%.2f", service.laborCost)}", fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("TOTAL FACTURADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "$ ${String.format(Locale.US, "%.2f", service.totalCost)}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Next interval
                                    if (service.nextServiceMileage > 0 || service.nextServiceDate > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF3C7),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "🎯 Próximo Mantenimiento:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFB45309)
                                                )
                                                if (service.nextServiceMileage > 0) {
                                                    Text("• Meta: ${service.nextServiceMileage} km", fontSize = 12.sp, color = Color(0xFF1E293B))
                                                }
                                                if (service.nextServiceDate > 0) {
                                                    Text("• Fecha: ${dateOnlyFormat.format(Date(service.nextServiceDate))}", fontSize = 12.sp, color = Color(0xFF1E293B))
                                                }
                                            }
                                        }
                                    }

                                    if (service.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Notas / Recomendaciones: ${service.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Sin mantenimientos registrados para este vehículo", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Action Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // PDF Button
                            if (service != null) {
                                Button(
                                    onClick = {
                                        val pdfFile = PdfReportService.generateServiceTicketPdf(context, client, vehicle, service)
                                        if (pdfFile != null && pdfFile.exists()) {
                                            PdfReportService.sharePdf(context, pdfFile, "Orden de Servicio ${vehicle.plateNumber}")
                                        } else {
                                            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("detail_share_pdf_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quote Button
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenQuote(client, vehicle, service)
                                },
                                modifier = Modifier.weight(1.1f).height(44.dp).testTag("detail_create_quote_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cotizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // WhatsApp Button
                            Button(
                                onClick = { shareRecordViaWhatsApp() },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("detail_share_whatsapp_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoGreen)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Update Maintenance Button
                            Button(
                                onClick = {
                                    onDismiss()
                                    onEditService(vehicle)
                                },
                                modifier = Modifier.weight(1.1f).height(44.dp).testTag("detail_update_service_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Servicio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
