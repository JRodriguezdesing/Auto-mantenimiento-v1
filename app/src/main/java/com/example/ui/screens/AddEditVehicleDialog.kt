package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Client
import com.example.data.model.Vehicle

@Composable
fun AddEditVehicleDialog(
    clientId: Long,
    clientName: String = "",
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var plateNumber by remember { mutableStateOf(vehicle?.plateNumber ?: "") }
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var yearStr by remember { mutableStateOf(vehicle?.year?.toString() ?: "2022") }
    var mileageStr by remember { mutableStateOf(vehicle?.currentMileage?.toString() ?: "") }
    var color by remember { mutableStateOf(vehicle?.color ?: "") }
    var engineType by remember { mutableStateOf(vehicle?.engineType ?: "Gasolina") }
    var notes by remember { mutableStateOf(vehicle?.notes ?: "") }

    var isError by remember { mutableStateOf(false) }

    val fuelTypes = listOf("Gasolina", "Diésel", "Híbrido", "Eléctrico", "GLP/GNV")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (vehicle == null) "Nuevo Vehículo" else "Editar Vehículo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                if (clientName.isNotBlank()) {
                    Text(
                        text = "Cliente: $clientName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = {
                        plateNumber = it.uppercase()
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Placa / Matrícula *") },
                    placeholder = { Text("ej. PBX-4592") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    isError = isError && plateNumber.isBlank(),
                    supportingText = {
                        if (isError && plateNumber.isBlank()) Text("La placa es obligatoria")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vehicle_plate_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca *") },
                        placeholder = { Text("Toyota, Chevrolet...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_brand_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo *") },
                        placeholder = { Text("Hilux, Tracker...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vehicle_model_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Año") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = mileageStr,
                        onValueChange = { mileageStr = it },
                        label = { Text("Km Actual *") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("vehicle_mileage_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color") },
                    placeholder = { Text("ej. Blanco Perlado, Gris") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tipo de Combustible / Motor:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fuelTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = engineType == type,
                            onClick = { engineType = type },
                            label = { Text(type) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fuelTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = engineType == type,
                            onClick = { engineType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Detalles Adicionales") },
                    placeholder = { Text("ej. Motor 2.8L turbo, caja automática...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (plateNumber.isBlank() || brand.isBlank() || model.isBlank()) {
                        isError = true
                    } else {
                        val parsedYear = yearStr.toIntOrNull() ?: 2022
                        val parsedMileage = mileageStr.toIntOrNull() ?: 0
                        onSave(
                            Vehicle(
                                id = vehicle?.id ?: 0L,
                                clientId = clientId,
                                plateNumber = plateNumber.trim(),
                                brand = brand.trim(),
                                model = model.trim(),
                                year = parsedYear,
                                currentMileage = parsedMileage,
                                color = color.trim(),
                                engineType = engineType,
                                notes = notes.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_vehicle_button")
            ) {
                Text("Guardar Auto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
