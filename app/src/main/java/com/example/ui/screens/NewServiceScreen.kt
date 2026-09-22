package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceRecord
import com.example.data.model.VehicleWithClient
import com.example.ui.theme.AutoAmber
import com.example.ui.theme.AutoBlue
import com.example.ui.theme.AutoGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewServiceScreen(
    vehiclesWithClients: List<VehicleWithClient>,
    preselectedVehicleId: Long? = null,
    existingServices: List<ServiceRecord> = emptyList(),
    onSaveService: (ServiceRecord) -> Unit,
    onServiceSavedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Vehicle Selection
    var selectedVehicleWithClient by remember {
        mutableStateOf(vehiclesWithClients.find { it.vehicle.id == preselectedVehicleId } ?: vehiclesWithClients.firstOrNull())
    }

    LaunchedEffect(preselectedVehicleId, vehiclesWithClients) {
        if (preselectedVehicleId != null && preselectedVehicleId > 0) {
            val found = vehiclesWithClients.find { it.vehicle.id == preselectedVehicleId }
            if (found != null) {
                selectedVehicleWithClient = found
            }
        } else if (selectedVehicleWithClient == null && vehiclesWithClients.isNotEmpty()) {
            selectedVehicleWithClient = vehiclesWithClients.firstOrNull()
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Service Date State (Modifiable by user)
    var serviceDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Mileage
    var mileageStr by remember { mutableStateOf("") }

    // MULTI-SERVICE WORKSHOP CATALOG
    val availableServices = remember {
        mutableStateListOf(
            "Cambio de Aceite y Filtro",
            "Frenos y Pastillas",
            "Alineación y Balanceo",
            "Afinamiento y Bujías",
            "Filtro de Aire / Cabina",
            "Batería y Sistema Eléctrico",
            "Suspensión y Amortiguadores",
            "Limpieza de Inyectores",
            "Refrigerante y Termostato",
            "Transmisión y Embrague",
            "Aire Acondicionado",
            "Rotación y Calibración de Llantas",
            "Inspección General 30 Puntos"
        )
    }

    // Multi-selected service jobs
    val selectedServices = remember {
        mutableStateListOf("Cambio de Aceite y Filtro")
    }

    // Custom Service Type Dialog
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var newCustomServiceName by remember { mutableStateOf("") }

    // Editable text for overall service summary
    var serviceTypeSummary by remember { mutableStateOf("Cambio de Aceite y Filtro") }

    // Oil and Fluids
    var oilDetails by remember { mutableStateOf("5W-30 Sintético Mobil Super") }
    var partsDescription by remember { mutableStateOf("Filtro de aceite, arandela de tapón") }
    var partsCostStr by remember { mutableStateOf("45.00") }
    var laborCostStr by remember { mutableStateOf("15.00") }
    var notes by remember { mutableStateOf("") }
    var hasLoadedPastData by remember { mutableStateOf(false) }

    // Pre-populate with existing stored service when vehicle is loaded or chosen
    LaunchedEffect(selectedVehicleWithClient?.vehicle?.id, existingServices) {
        val vId = selectedVehicleWithClient?.vehicle?.id
        if (vId != null && existingServices.isNotEmpty()) {
            val pastService = existingServices.filter { it.vehicleId == vId }.maxByOrNull { it.serviceDate }
            if (pastService != null) {
                oilDetails = pastService.oilDetails
                partsDescription = pastService.partsDescription
                partsCostStr = if (pastService.partsCost > 0) String.format(Locale.US, "%.2f", pastService.partsCost) else partsCostStr
                laborCostStr = if (pastService.laborCost > 0) String.format(Locale.US, "%.2f", pastService.laborCost) else laborCostStr
                notes = pastService.notes
                serviceDateMillis = pastService.serviceDate
                mileageStr = pastService.mileageAtService.toString()

                if (pastService.serviceType.isNotBlank()) {
                    val parts = pastService.serviceType.split("+").map { it.trim() }.filter { it.isNotBlank() }
                    selectedServices.clear()
                    parts.forEach { p ->
                        if (!availableServices.contains(p)) availableServices.add(p)
                        selectedServices.add(p)
                    }
                    serviceTypeSummary = pastService.serviceType
                }
                hasLoadedPastData = true
            } else {
                mileageStr = selectedVehicleWithClient?.vehicle?.currentMileage?.toString() ?: ""
                hasLoadedPastData = false
            }
        } else if (selectedVehicleWithClient != null) {
            mileageStr = selectedVehicleWithClient!!.vehicle.currentMileage.toString()
            hasLoadedPastData = false
        }
    }

    // Update serviceTypeSummary whenever selection changes (if not manually edited)
    LaunchedEffect(selectedServices.toList()) {
        if (selectedServices.isNotEmpty()) {
            serviceTypeSummary = selectedServices.joinToString(" + ")
        }
    }

    val hasOilServiceSelected by remember {
        derivedStateOf {
            selectedServices.any { it.contains("Aceite", ignoreCase = true) }
        }
    }

    val totalCost by remember {
        derivedStateOf {
            val parts = partsCostStr.toDoubleOrNull() ?: 0.0
            val labor = laborCostStr.toDoubleOrNull() ?: 0.0
            parts + labor
        }
    }

    // Next Service Interval Calculations
    var kmIncrement by remember { mutableIntStateOf(5000) }
    var monthsIncrement by remember { mutableIntStateOf(6) }

    val nextMileage by remember {
        derivedStateOf {
            val current = mileageStr.toIntOrNull() ?: (selectedVehicleWithClient?.vehicle?.currentMileage ?: 0)
            current + kmIncrement
        }
    }

    val nextDate by remember {
        derivedStateOf {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, monthsIncrement)
            cal.timeInMillis
        }
    }

    val oilViscosities = listOf("0W-20", "5W-30", "5W-40", "10W-30", "10W-40", "15W-40", "20W-50")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("new_service_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Registrar Orden de Mantenimiento",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecciona múltiples trabajos, añade tipos personalizados y programa recordatorios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 1. Vehicle Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Vehículo y Cliente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (vehiclesWithClients.isEmpty()) {
                        Text(
                            text = "No hay vehículos registrados. Por favor registre un cliente y su vehículo primero.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedVehicleWithClient?.let {
                                    "[${it.vehicle.plateNumber}] ${it.vehicle.brand} ${it.vehicle.model} - ${it.client.name}"
                                } ?: "Seleccionar vehículo...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("vehicle_selector_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                vehiclesWithClients.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${item.vehicle.plateNumber} • ${item.vehicle.brand} ${item.vehicle.model}",
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Cliente: ${item.client.name} (Actual: ${item.vehicle.currentMileage} km)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedVehicleWithClient = item
                                            mileageStr = item.vehicle.currentMileage.toString()
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Picker (Editable Service Date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fecha del Servicio (Modificable):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormat.format(Date(serviceDateMillis)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = serviceDateMillis }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance()
                                        newCal.set(year, month, dayOfMonth)
                                        serviceDateMillis = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("change_service_date_btn")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar Fecha")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mileageStr,
                        onValueChange = { mileageStr = it },
                        label = { Text("Kilometraje al Ingresar (km) *") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_mileage_input"),
                        singleLine = true
                    )

                    if (hasLoadedPastData) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AutoBlue.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = AutoBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Datos cargados del último servicio de este vehículo. Todos los campos son editables.",
                                        fontSize = 11.sp,
                                        color = AutoBlue
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        oilDetails = ""
                                        partsDescription = ""
                                        partsCostStr = "0.00"
                                        laborCostStr = "0.00"
                                        notes = ""
                                        selectedServices.clear()
                                        selectedServices.add("Cambio de Aceite y Filtro")
                                        hasLoadedPastData = false
                                    }
                                ) {
                                    Text("Limpiar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Multi-Service Work Catalog & Custom Service Types
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "2. Trabajos y Tipos de Servicio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Puedes seleccionar varios trabajos realizados a la vez",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${selectedServices.size} selec.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chips Grid for all available services
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableServices.forEach { item ->
                            val isSelected = selectedServices.contains(item)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        if (selectedServices.size > 1) {
                                            selectedServices.remove(item)
                                        } else {
                                            selectedServices.remove(item)
                                        }
                                    } else {
                                        selectedServices.add(item)
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(item, fontSize = 12.sp) },
                                modifier = Modifier.testTag("service_chip_${item.replace(" ", "_")}")
                            )
                        }

                        // Button to add NEW custom service type
                        OutlinedButton(
                            onClick = { showAddCustomDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_custom_service_type_btn")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Agregar Otro Tipo de Trabajo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Editable combined description
                    OutlinedTextField(
                        value = serviceTypeSummary,
                        onValueChange = { serviceTypeSummary = it },
                        label = { Text("Resumen de Trabajos a Facturar / Registrar") },
                        supportingText = { Text("Se auto-genera de los trabajos seleccionados, editable libremente") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_type_input"),
                        maxLines = 3
                    )
                }
            }
        }

        // 3. Oil, Fluids & Replaced Parts
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.OilBarrel,
                            contentDescription = null,
                            tint = if (hasOilServiceSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Aceite, Fluidos y Repuestos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!hasOilServiceSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nota: No se seleccionó cambio de aceite, pero puedes registrar lubricantes o fluidos si se utilizaron.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Viscosidad sugerida de lubricante:", style = MaterialTheme.typography.bodySmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        oilViscosities.forEach { visc ->
                            FilterChip(
                                selected = oilDetails.contains(visc),
                                onClick = {
                                    oilDetails = if (oilDetails.isBlank()) visc else "$visc ${oilDetails.replace(Regex("\\b\\d+W-\\d+\\b"), "").trim()}"
                                },
                                label = { Text(visc, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = oilDetails,
                        onValueChange = { oilDetails = it },
                        label = { Text("Detalle de Aceite / Fluido") },
                        placeholder = { Text("ej. 5W-30 Sintético Mobil Super (4.5 Qt) o N/A") },
                        leadingIcon = { Icon(Icons.Default.OilBarrel, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("oil_details_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = partsDescription,
                        onValueChange = { partsDescription = it },
                        label = { Text("Repuestos y Materiales Reemplazados") },
                        placeholder = { Text("ej. Pastillas delanteras Brembo, filtro de aire, bujías NGK...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parts_description_input"),
                        maxLines = 3
                    )
                }
            }
        }

        // 4. Costs & Invoicing
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Costos y Facturación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = partsCostStr,
                            onValueChange = { partsCostStr = it },
                            label = { Text("Repuestos ($)") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("parts_cost_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = laborCostStr,
                            onValueChange = { laborCostStr = it },
                            label = { Text("Mano de Obra ($)") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("labor_cost_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Costo Total del Servicio:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$ ${String.format(Locale.US, "%.2f", totalCost)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // 5. Automatic Next Reminder Programming
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "5. Programación de Próximo Recordatorio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configura el intervalo para generar alertas y avisos automáticos a este cliente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Kilómetros para el siguiente servicio:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5000, 7500, 10000, 15000).forEach { km ->
                            FilterChip(
                                selected = kmIncrement == km,
                                onClick = { kmIncrement = km },
                                label = { Text("+$km km", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Plazo de tiempo estimado:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3 to "+3 Meses", 6 to "+6 Meses", 12 to "+1 Año").forEach { (months, label) ->
                            FilterChip(
                                selected = monthsIncrement == months,
                                onClick = { monthsIncrement = months },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AutoAmber.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎯 Próximo Mantenimiento Programado:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AutoAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Kilometraje meta: $nextMileage km\n• Fecha estimada: ${dateFormat.format(Date(nextDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observaciones / Recomendaciones para el Cliente") },
                        placeholder = { Text("ej. Próxima visita chequear desgaste de pastillas y rotación") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val currentSelection = selectedVehicleWithClient
                    if (currentSelection == null) {
                        Toast.makeText(context, "Debe seleccionar un vehículo", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val mileage = mileageStr.toIntOrNull() ?: currentSelection.vehicle.currentMileage
                    val finalServiceType = serviceTypeSummary.trim().ifBlank {
                        selectedServices.joinToString(" + ").ifBlank { "Mantenimiento General" }
                    }

                    val service = ServiceRecord(
                        vehicleId = currentSelection.vehicle.id,
                        clientId = currentSelection.client.id,
                        serviceDate = serviceDateMillis,
                        mileageAtService = mileage,
                        serviceType = finalServiceType,
                        oilDetails = oilDetails.trim(),
                        partsDescription = partsDescription.trim(),
                        partsCost = partsCostStr.toDoubleOrNull() ?: 0.0,
                        laborCost = laborCostStr.toDoubleOrNull() ?: 0.0,
                        totalCost = totalCost,
                        nextServiceMileage = nextMileage,
                        nextServiceDate = nextDate,
                        notes = notes.trim(),
                        isSyncedToSheets = false
                    )

                    onSaveService(service)
                    Toast.makeText(context, "¡Mantenimiento registrado con éxito!", Toast.LENGTH_LONG).show()
                    onServiceSavedSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_service_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Mantenimiento y Programar Alerta",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialog for adding a custom service type
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCustomDialog = false
                newCustomServiceName = ""
            },
            title = {
                Text("Nuevo Tipo de Trabajo / Servicio", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Escribe el nombre del trabajo técnico a añadir al catálogo de este taller:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newCustomServiceName,
                        onValueChange = { newCustomServiceName = it },
                        label = { Text("Nombre del Trabajo") },
                        placeholder = { Text("ej. Cambio de Correa de Distribución") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_service_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCustomServiceName.trim()
                        if (trimmed.isNotBlank()) {
                            if (!availableServices.contains(trimmed)) {
                                availableServices.add(trimmed)
                            }
                            if (!selectedServices.contains(trimmed)) {
                                selectedServices.add(trimmed)
                            }
                            Toast.makeText(context, "Trabajo '$trimmed' agregado", Toast.LENGTH_SHORT).show()
                            newCustomServiceName = ""
                            showAddCustomDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_custom_service_btn")
                ) {
                    Text("Agregar y Seleccionar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomDialog = false
                    newCustomServiceName = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
