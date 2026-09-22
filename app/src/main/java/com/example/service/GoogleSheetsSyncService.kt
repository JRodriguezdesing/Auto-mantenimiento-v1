package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Client
import com.example.data.model.ServiceRecord
import com.example.data.model.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetsSyncService(private val context: Context) {

    private val prefs = context.getSharedPreferences("auto_mantenimiento_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getWebhookUrl(): String {
        return prefs.getString("google_sheets_webhook_url", "") ?: ""
    }

    fun saveWebhookUrl(url: String) {
        prefs.edit().putString("google_sheets_webhook_url", url.trim()).apply()
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong("last_sync_timestamp", 0L)
    }

    fun getLastSyncStatus(): String {
        return prefs.getString("last_sync_status", "Sin sincronizaciones previas") ?: "Sin sincronizaciones previas"
    }

    private fun updateSyncStatus(status: String) {
        prefs.edit()
            .putLong("last_sync_timestamp", System.currentTimeMillis())
            .putString("last_sync_status", status)
            .apply()
    }

    suspend fun syncWithWebhook(
        clients: List<Client>,
        vehicles: List<Vehicle>,
        services: List<ServiceRecord>
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = getWebhookUrl()
        if (url.isBlank()) {
            val msg = "Configura la URL de tu Google Apps Script en los ajustes."
            updateSyncStatus("Error: URL vacía")
            return@withContext Result.failure(Exception(msg))
        }

        try {
            val clientMap = clients.associateBy { it.id }
            val vehicleMap = vehicles.associateBy { it.id }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val rootJson = JSONObject().apply {
                put("action", "AUTO_MAINTENANCE_BACKUP")
                put("exportedAt", dateFormat.format(Date()))
                put("appVersion", "2.0")

                // Clients array
                val clientsArray = JSONArray()
                clients.forEach { c ->
                    clientsArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("email", c.email)
                        put("documentId", c.documentId)
                        put("address", c.address)
                        put("notes", c.notes)
                    })
                }
                put("clients", clientsArray)

                // Vehicles array
                val vehiclesArray = JSONArray()
                vehicles.forEach { v ->
                    val owner = clientMap[v.clientId]
                    vehiclesArray.put(JSONObject().apply {
                        put("id", v.id)
                        put("clientId", v.clientId)
                        put("clientName", owner?.name ?: "Desconocido")
                        put("plateNumber", v.plateNumber)
                        put("brand", v.brand)
                        put("model", v.model)
                        put("year", v.year)
                        put("currentMileage", v.currentMileage)
                        put("color", v.color)
                        put("engineType", v.engineType)
                    })
                }
                put("vehicles", vehiclesArray)

                // Services array
                val servicesArray = JSONArray()
                services.forEach { s ->
                    val v = vehicleMap[s.vehicleId]
                    val c = clientMap[s.clientId]
                    servicesArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("clientName", c?.name ?: "")
                        put("clientPhone", c?.phone ?: "")
                        put("vehiclePlate", v?.plateNumber ?: "")
                        put("vehicleModel", "${v?.brand ?: ""} ${v?.model ?: ""}".trim())
                        put("serviceDate", dateFormat.format(Date(s.serviceDate)))
                        put("mileageAtService", s.mileageAtService)
                        put("serviceType", s.serviceType)
                        put("oilDetails", s.oilDetails)
                        put("partsDescription", s.partsDescription)
                        put("partsCost", s.partsCost)
                        put("laborCost", s.laborCost)
                        put("totalCost", s.totalCost)
                        put("nextServiceMileage", s.nextServiceMileage)
                        put("nextServiceDate", if (s.nextServiceDate > 0) dateFormat.format(Date(s.nextServiceDate)) else "")
                        put("notes", s.notes)
                    })
                }
                put("services", servicesArray)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val successMsg = "Respaldo exitoso a Google Sheets (${services.size} servicios, ${vehicles.size} autos)"
                updateSyncStatus(successMsg)
                Result.success(successMsg)
            } else {
                val errorMsg = "Error en el servidor de Google Sheets: HTTP ${response.code}"
                updateSyncStatus(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Fallo de conexión: ${e.localizedMessage ?: "Error desconocido"}"
            updateSyncStatus(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Generate and share PDF Report of all services
     */
    fun generateAndSharePdf(
        clients: List<Client>,
        vehicles: List<Vehicle>,
        services: List<ServiceRecord>
    ) {
        val pdfFile = PdfReportService.generateAllServicesSummaryPdf(context, services, vehicles, clients)
        if (pdfFile != null && pdfFile.exists()) {
            PdfReportService.sharePdf(context, pdfFile, "Informe Consolidado AUTO MANTENIMIENTO")
            updateSyncStatus("Exportado informe PDF (${services.size} servicios)")
        } else {
            Toast.makeText(context, "No se pudo generar el archivo PDF", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Safe CSV generator and sharing that does NOT crash the application
     */
    fun generateAndShareCsv(
        clients: List<Client>,
        vehicles: List<Vehicle>,
        services: List<ServiceRecord>
    ) {
        val clientMap = clients.associateBy { it.id }
        val vehicleMap = vehicles.associateBy { it.id }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        // Header with UTF-8 BOM
        sb.append("\uFEFF") // Excel UTF-8 BOM
        sb.append("ID,Fecha Servicio,Cliente,Telefono,Email,Placa,Marca,Modelo,Anio,Km Servicio,Trabajos Realizados,Detalle Aceite,Repuestos,Costo Repuestos ($),Mano de Obra ($),Total Facturado ($),Proximo Km,Proxima Fecha,Observaciones\n")

        for (s in services) {
            val v = vehicleMap[s.vehicleId]
            val c = clientMap[s.clientId]

            val dateStr = dateFormat.format(Date(s.serviceDate))
            val nextDateStr = if (s.nextServiceDate > 0) dateFormat.format(Date(s.nextServiceDate)) else "N/A"

            sb.append("${s.id},")
            sb.append("\"$dateStr\",")
            sb.append("\"${(c?.name ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(c?.phone ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(c?.email ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(v?.plateNumber ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(v?.brand ?: "").replace("\"", "\"\"")}\",")
            sb.append("\"${(v?.model ?: "").replace("\"", "\"\"")}\",")
            sb.append("${v?.year ?: ""},")
            sb.append("${s.mileageAtService},")
            sb.append("\"${s.serviceType.replace("\"", "\"\"")}\",")
            sb.append("\"${s.oilDetails.replace("\"", "\"\"")}\",")
            sb.append("\"${s.partsDescription.replace("\"", "\"\"")}\",")
            sb.append("${s.partsCost},")
            sb.append("${s.laborCost},")
            sb.append("${s.totalCost},")
            sb.append("${s.nextServiceMileage},")
            sb.append("\"$nextDateStr\",")
            sb.append("\"${s.notes.replace("\"", "\"\"")}\"\n")
        }

        try {
            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(cacheDir, "mantenimientos_autos_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use {
                it.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Respaldo AUTO MANTENIMIENTO CSV")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Abrir o compartir respaldo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            updateSyncStatus("Exportado CSV (${services.size} registros)")
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Fallback share plain text safely
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    putExtra(Intent.EXTRA_SUBJECT, "Respaldo AUTO MANTENIMIENTO")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "Compartir datos").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "No hay aplicaciones disponibles para compartir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val GOOGLE_APPS_SCRIPT_SAMPLE = """
/**
 * GOOGLE APPS SCRIPT PROFESIONAL PARA: AUTO MANTENIMIENTO
 * 
 * INSTRUCCIONES:
 * 1. En Google Sheets ve a: Extensiones > Apps Script
 * 2. Borra el código existente, pega este bloque y pulsa Guardar (Ctrl + S)
 * 3. Haz clic en "Implementar" > "Nueva implementación"
 * 4. Tipo: "Aplicación web"
 * 5. Ejecutar como: "Yo" (tu cuenta)
 * 6. Quién tiene acceso: "Cualquier persona" (Anyone)
 * 7. Copia la URL de la aplicación web y pégala en la app.
 */
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // Buscar o configurar la hoja "Historial_Servicios"
    var sheet = ss.getSheetByName("Historial_Servicios");
    if (!sheet) {
      var firstSheet = ss.getSheets()[0];
      if (firstSheet && (firstSheet.getName() === "Hoja 1" || firstSheet.getName() === "Sheet1") && firstSheet.getLastRow() <= 1) {
        firstSheet.setName("Historial_Servicios");
        sheet = firstSheet;
      } else {
        sheet = ss.insertSheet("Historial_Servicios");
      }
    }
    
    // Encabezados con formato visual y colores
    var headers = [
      "ID", "Fecha Servicio", "Cliente", "Teléfono", "Placa", "Vehículo", "Kilometraje", 
      "Trabajos Realizados", "Aceite / Fluidos", "Repuestos Reemplazados", 
      "Costo Repuestos ($)", "Mano de Obra ($)", "Total Facturado ($)", 
      "Próximo Km", "Próxima Fecha", "Observaciones"
    ];
    
    // Si la hoja no tiene encabezados o está vacía, escribirlos y dar formato con diseño
    var lastRow = sheet.getLastRow();
    if (lastRow === 0 || sheet.getRange(1, 1).getValue() === "") {
      sheet.clear(); // Limpiar residuos o desalineaciones
      sheet.appendRow(headers);
      
      var headerRange = sheet.getRange(1, 1, 1, headers.length);
      headerRange.setBackground("#1E3A8A"); // Azul Marino Taller
      headerRange.setFontColor("#FFFFFF");
      headerRange.setFontWeight("bold");
      headerRange.setFontSize(10.5);
      headerRange.setHorizontalAlignment("center");
      headerRange.setVerticalAlignment("middle");
      sheet.setRowHeight(1, 35);
      sheet.setFrozenRows(1);
    }
    
    // Obtener IDs ya registrados para evitar duplicados en la hoja
    var existingIds = {};
    lastRow = sheet.getLastRow();
    if (lastRow > 1) {
      var idValues = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
      for (var i = 0; i < idValues.length; i++) {
        var idStr = idValues[i][0] ? idValues[i][0].toString() : "";
        if (idStr) existingIds[idStr] = true;
      }
    }
    
    // Filtrar y preparar solo nuevos registros
    var newRows = [];
    if (data.services && data.services.length > 0) {
      data.services.forEach(function(s) {
        var sid = s.id ? s.id.toString() : "";
        if (!existingIds[sid]) {
          newRows.push([
            s.id || "",
            s.serviceDate || "",
            s.clientName || "",
            s.clientPhone || "",
            s.vehiclePlate || "",
            s.vehicleModel || "",
            s.mileageAtService || 0,
            s.serviceType || "",
            s.oilDetails || "N/A",
            s.partsDescription || "",
            s.partsCost || 0,
            s.laborCost || 0,
            s.totalCost || 0,
            s.nextServiceMileage || 0,
            s.nextServiceDate || "",
            s.notes || ""
          ]);
        }
      });
    }
    
    if (newRows.length > 0) {
      var startRow = sheet.getLastRow() + 1;
      var insertRange = sheet.getRange(startRow, 1, newRows.length, headers.length);
      insertRange.setValues(newRows);
      
      // Aplicar formato de moneda a columnas 11, 12 y 13
      var updatedLastRow = sheet.getLastRow();
      sheet.getRange(2, 11, updatedLastRow - 1, 3).setNumberFormat("$#,##0.00");
      // Formato numérico a kilometrajes
      sheet.getRange(2, 7, updatedLastRow - 1, 1).setNumberFormat("#,##0");
      sheet.getRange(2, 14, updatedLastRow - 1, 1).setNumberFormat("#,##0");
    }
    
    // Auto-ajustar columnas
    for (var c = 1; c <= headers.length; c++) {
      sheet.autoResizeColumn(c);
    }
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      message: "Respaldo procesado con encabezados y diseño profesional",
      newRowsCount: newRows.length
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
"""
    }
}
