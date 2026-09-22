package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Client
import com.example.data.model.MaintenanceQuote
import com.example.data.model.QuoteMaterialItem
import com.example.data.model.ServiceRecord
import com.example.data.model.Vehicle
import com.example.service.PdfReportService
import com.example.ui.theme.AutoAmber
import com.example.ui.theme.AutoBlue
import com.example.ui.theme.AutoGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuoteDialog(
    client: Client?,
    vehicle: Vehicle?,
    service: ServiceRecord? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var quoteDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var clientName by remember { mutableStateOf(client?.name ?: "") }
    var clientPhone by remember { mutableStateOf(client?.phone ?: "") }
    var vehiclePlate by remember { mutableStateOf(vehicle?.plateNumber ?: "") }
    var vehicleModel by remember { mutableStateOf("${vehicle?.brand ?: ""} ${vehicle?.model ?: ""}".trim()) }
    var currentMileage by remember { mutableStateOf(vehicle?.currentMileage?.toString() ?: (service?.mileageAtService?.toString() ?: "0")) }

    // Initial items based on service if provided
    val initialItems = remember(service) {
        val list = mutableListOf<QuoteMaterialItem>()
        if (service != null && service.partsDescription.isNotBlank()) {
            val parts = service.partsDescription.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
            val unitPartCost = if (parts.isNotEmpty() && service.partsCost > 0) service.partsCost / parts.size else 25.0
            parts.forEach { p ->
                list.add(QuoteMaterialItem(name = p, quantity = 1.0, unitPrice = unitPartCost))
            }
        }
        if (list.isEmpty()) {
            list.add(QuoteMaterialItem(name = "Filtro de Aceite", quantity = 1.0, unitPrice = 12.00))
            list.add(QuoteMaterialItem(name = "Aceite de Motor (Galón / Cuartos)", quantity = 1.0, unitPrice = 35.00))
        }
        list
    }

    val materials = remember { mutableStateListOf<QuoteMaterialItem>().apply { addAll(initialItems) } }

    var laborRole by remember { mutableStateOf("Mecánico General") }
    var laborDescription by remember { mutableStateOf("Instalación de repuestos, calibración y prueba técnica") }
    var laborCostStr by remember { mutableStateOf(if (service != null && service.laborCost > 0) String.format(Locale.US, "%.2f", service.laborCost) else "25.00") }
    var validityDays by remember { mutableStateOf("15") }
    var notes by remember { mutableStateOf("Precios incluyen repuestos autorizados. Cotización válida por 15 días.") }

    val materialsTotal by remember {
        derivedStateOf { materials.sumOf { it.subtotal } }
    }

    val grandTotal by remember {
        derivedStateOf {
            val labor = laborCostStr.toDoubleOrNull() ?: 0.0
            materialsTotal + labor
        }
    }

    // DatePicker trigger
    fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = quoteDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth)
                quoteDateMillis = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun buildQuoteObject(): MaintenanceQuote {
        return MaintenanceQuote(
            quoteNumber = "COT-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(quoteDateMillis)),
            date = quoteDateMillis,
            clientName = clientName.trim(),
            clientPhone = clientPhone.trim(),
            vehiclePlate = vehiclePlate.trim().uppercase(Locale.getDefault()),
            vehicleModel = vehicleModel.trim(),
            currentMileage = currentMileage.toIntOrNull() ?: 0,
            materials = materials.toMutableList(),
            laborRole = laborRole,
            laborDescription = laborDescription.trim(),
            laborCost = laborCostStr.toDoubleOrNull() ?: 0.0,
            validityDays = validityDays.toIntOrNull() ?: 15,
            notes = notes.trim()
        )
    }

    fun shareQuoteViaWhatsApp() {
        val quote = buildQuoteObject()
        val sb = StringBuilder()
        sb.append("📋 *AUTO MANTENIMIENTO - COTIZACIÓN DE MATERIALES Y MANO DE OBRA*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📅 *Fecha:* ${dateFormat.format(Date(quote.date))}\n")
        sb.append("👤 *Cliente:* ${quote.clientName}\n")
        sb.append("🚗 *Vehículo:* ${quote.vehiclePlate} (${quote.vehicleModel})\n")
        sb.append("📍 *Km:* ${quote.currentMileage} km\n\n")

        sb.append("🔩 *MATERIALES Y REPUESTOS AUTORIZADOS:*\n")
        if (quote.materials.isEmpty()) {
            sb.append("• Sin repuestos (Solo mano de obra)\n")
        } else {
            quote.materials.forEachIndexed { idx, m ->
                val qty = if (m.quantity % 1.0 == 0.0) m.quantity.toInt().toString() else m.quantity.toString()
                sb.append("${idx + 1}. *${m.name}* (Cant: $qty x $${String.format(Locale.US, "%.2f", m.unitPrice)}) = *$${String.format(Locale.US, "%.2f", m.subtotal)}*\n")
            }
        }
        sb.append("▫ Subtotal Materiales: *$${String.format(Locale.US, "%.2f", quote.materialsTotal)}*\n\n")

        sb.append("🔧 *MANO DE OBRA (${quote.laborRole}):*\n")
        sb.append("• ${quote.laborDescription}\n")
        sb.append("▫ Valor Mano de Obra: *$${String.format(Locale.US, "%.2f", quote.laborCost)}*\n\n")

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *TOTAL COTIZACIÓN: $${String.format(Locale.US, "%.2f", quote.grandTotal)}*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("⏳ *Validez:* ${quote.validityDays} días.\n")
        sb.append("📝 *Nota:* ${quote.notes}\n\n")
        sb.append("¿Desea autorizar el inicio de los trabajos? Quedamos a su disposición.")

        val message = sb.toString()
        val cleanPhone = quote.clientPhone.replace(Regex("[^0-9+]"), "")

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (cleanPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Cotización").apply {
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
                .fillMaxHeight(0.92f)
                .testTag("quote_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
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
                                    text = "Cotización de Materiales & Mano de Obra",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Date & Vehicle info card
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "1. Fecha y Datos del Cliente",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    OutlinedButton(
                                        onClick = { showDatePicker() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(dateFormat.format(Date(quoteDateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = clientName,
                                        onValueChange = { clientName = it },
                                        label = { Text("Cliente") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = clientPhone,
                                        onValueChange = { clientPhone = it },
                                        label = { Text("Teléfono / WhatsApp") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = vehiclePlate,
                                        onValueChange = { vehiclePlate = it.uppercase(Locale.getDefault()) },
                                        label = { Text("Placa") },
                                        modifier = Modifier.weight(0.9f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = vehicleModel,
                                        onValueChange = { vehicleModel = it },
                                        label = { Text("Vehículo") },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = currentMileage,
                                        onValueChange = { currentMileage = it },
                                        label = { Text("Km") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Materials & Parts List
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "2. Materiales y Repuestos Requeridos",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Agrega solo los materiales autorizados por el cliente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    materials.add(QuoteMaterialItem(name = "", quantity = 1.0, unitPrice = 0.0))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("add_quote_item_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Repuesto", fontSize = 11.sp)
                            }
                        }
                    }

                    // Items in Materials
                    itemsIndexed(materials, key = { _, item -> item.id }) { index, item ->
                        var nameState by remember(item.id) { mutableStateOf(item.name) }
                        var qtyState by remember(item.id) { mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()) }
                        var priceState by remember(item.id) { mutableStateOf(if (item.unitPrice > 0) String.format(Locale.US, "%.2f", item.unitPrice) else "") }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = nameState,
                                        onValueChange = {
                                            nameState = it
                                            item.name = it
                                        },
                                        label = { Text("Repuesto / Material #${index + 1}") },
                                        placeholder = { Text("ej. Pastillas delanteras Brembo") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    IconButton(
                                        onClick = { materials.removeAt(index) },
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = qtyState,
                                        onValueChange = {
                                            qtyState = it
                                            item.quantity = it.toDoubleOrNull() ?: 1.0
                                        },
                                        label = { Text("Cant.") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.8f),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = priceState,
                                        onValueChange = {
                                            priceState = it
                                            item.unitPrice = it.toDoubleOrNull() ?: 0.0
                                        },
                                        label = { Text("P. Unit ($)") },
                                        prefix = { Text("$ ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f).height(54.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text("Subtotal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Text(
                                                text = "$ ${String.format(Locale.US, "%.2f", item.subtotal)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Labor Section (Mano de Obra)
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "3. Mano de Obra Técnica",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Especialidad técnica:", style = MaterialTheme.typography.bodySmall)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Mecánico General", "Electromecánico", "Mecánico & Electromecánico", "Técnico Especialista").forEach { role ->
                                        FilterChip(
                                            selected = laborRole == role,
                                            onClick = { laborRole = role },
                                            label = { Text(role, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = laborDescription,
                                    onValueChange = { laborDescription = it },
                                    label = { Text("Descripción del trabajo de mano de obra") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = laborCostStr,
                                    onValueChange = { laborCostStr = it },
                                    label = { Text("Valor Mano de Obra ($)") },
                                    prefix = { Text("$ ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Totals & Terms
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal Repuestos:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$ ${String.format(Locale.US, "%.2f", materialsTotal)}", color = Color.White, fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Mano de Obra ($laborRole):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                    Text("$ ${String.format(Locale.US, "%.2f", laborCostStr.toDoubleOrNull() ?: 0.0)}", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0xFF334155))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TOTAL COTIZACIÓN:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "$ ${String.format(Locale.US, "%.2f", grandTotal)}",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Validity & Notes
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = validityDays,
                                onValueChange = { validityDays = it },
                                label = { Text("Validez (días)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Condiciones / Garantía") },
                                modifier = Modifier.weight(2f),
                                maxLines = 2
                            )
                        }
                    }
                }

                // Action Buttons Bottom Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button PDF
                            Button(
                                onClick = {
                                    val quote = buildQuoteObject()
                                    val pdfFile = PdfReportService.generateQuotePdf(context, quote)
                                    if (pdfFile != null && pdfFile.exists()) {
                                        PdfReportService.sharePdf(context, pdfFile, "Cotización ${quote.vehiclePlate} - AUTO MANTENIMIENTO")
                                        Toast.makeText(context, "Cotización PDF generada", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al generar PDF de cotización", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_quote_pdf_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartir PDF", fontWeight = FontWeight.Bold)
                            }

                            // Button WhatsApp
                            Button(
                                onClick = { shareQuoteViaWhatsApp() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_quote_whatsapp_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AutoGreen)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
