# 🚗 AUTO MANTENIMIENTO - Gestión Técnica, Cotizaciones y Recordatorios Automotrices

Aplicación móvil nativa Android para talleres mecánicos, centros de lubricación, electromecánica y gestores de flotas vehiculares. Desarrollada con **Kotlin** y **Jetpack Compose (Material Design 3)**, bajo una arquitectura **MVVM (Model-View-ViewModel)** robusta, reactiva y offline-first con base de datos local **Room (SQLite)**, generación de reportes y cotizaciones en **PDF a color**, y respaldo sincronizado en la nube con **Google Sheets**.

---

## 📋 Resumen Ejecutivo y Características Principales

1. **Gestión de Clientes y Flota Vehicular**:
   - Registro y edición de clientes (Nombre, WhatsApp, Email, Cédula/RUC, Dirección y Notas).
   - Gestión de múltiples vehículos por cliente con control de Placa, Marca, Modelo, Año, Kilometraje actual y Tipo de Motorización.

2. **Registro y Edición Flexible de Órdenes de Mantenimiento**:
   - Selector interactivo de fecha con calendario (`DatePickerDialog`).
   - Carga automática de los datos del último servicio del vehículo (aceite, repuestos, costos, notas) editable en todo momento.
   - Catálogo de múltiples tipos de servicios técnicos (Frenos, Afinamiento, Suspensión, Alineación, Eléctrico, Aire Acondicionado, etc.) con capacidad de agregar trabajos personalizados.
   - Desglose de costos: Repuestos + Mano de Obra (Mecánico / Electromecánico).

3. **Generación de Reportes y Cotizaciones en PDF**:
   - **Reporte Técnico en PDF**: Formato visual con colores institucionales, datos del cliente, vehículo, detalles del trabajo y advertencias de próximos servicios.
   - **Cotización de Materiales & Mano de Obra en PDF**: Módulo independiente para presupuestar repuestos autorizados por cantidad y valor unitario más la mano de obra del mecánico o electromecánico, listo para enviar directamente al cliente por WhatsApp o Correo.
   - **Ficha Técnica Visual para Captura**: Modal estilizado para visualizar todos los registros y tomar capturas de pantalla organizadas.

4. **Motor Automático de Alertas y Recordatorios**:
   - Monitoreo de kilometraje y fechas programadas con estados visuales:
     - 🔴 **VENCIDO**: Kilometraje o fecha límite superada.
     - 🟡 **PRÓXIMO**: A menos de 1,000 km o 15 días del servicio.
     - 🟢 **AL DÍA**: Servicio en orden.

5. **Automatización de Mensajería y Notificaciones Push**:
   - **WhatsApp**: Botón de un toque para enviar recordatorios personalizados y cotizaciones al cliente.
   - **Correo Electrónico**: Generación automática de reportes técnicos detallados.
   - **Notificaciones Push Nativas**: Canal prioritario del sistema operativo Android.

6. **Respaldo en la Nube con Google Sheets & Exportación**:
   - Sincronización directa vía Webhook HTTP a Google Sheets con creación automática de encabezados y títulos en la hoja.
   - Exportación segura en CSV y PDF compartibles a través de Android FileProvider.

---

## 🏛️ Estructura del Proyecto

```text
/
├── README.md                           <- Documentación técnica y guía de despliegue
├── metadata.json                       <- Configuración de plataforma AI Studio
├── app/
│   ├── build.gradle.kts                <- Dependencias, Room KSP, Compose M3, OkHttp
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     <- Permisos (INTERNET, POST_NOTIFICATIONS, VIBRATE) y FileProvider
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt     <- Punto de entrada, Scaffold y barra de navegación M3
│       │   │   ├── data/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Client.kt             <- Entidad Room para clientes
│       │   │   │   │   ├── Vehicle.kt            <- Entidad Room para vehículos
│       │   │   │   │   ├── ServiceRecord.kt      <- Entidad Room para órdenes de servicio
│       │   │   │   │   └── MaintenanceAlert.kt   <- Modelo de cálculo de alertas y estados
│       │   │   │   ├── local/
│       │   │   │   │   ├── ClientDao.kt          <- DAO de clientes
│       │   │   │   │   ├── VehicleDao.kt         <- DAO de vehículos
│       │   │   │   │   ├── ServiceRecordDao.kt   <- DAO de historial de servicios
│       │   │   │   │   └── AppDatabase.kt        <- Base de datos Room con migración y RoomDatabase
│       │   │   │   └── repository/
│       │   │   │       └── AutoRepository.kt     <- Capa de datos con Flow reactivo y cálculo de alertas
│       │   │   ├── service/
│       │   │   │   ├── NotificationHelper.kt      <- Canales y despacho de notificaciones Push
│       │   │   │   ├── ReminderMessagingHelper.kt <- Automatización de intents de WhatsApp y Correo
│       │   │   │   └── GoogleSheetsSyncService.kt <- Respaldo Webhook JSON y generador de CSV
│       │   │   └── ui/
│       │   │       ├── MainViewModel.kt      <- ViewModel central con StateFlows y KPIs
│       │   │       ├── screens/
│       │   │       │   ├── DashboardScreen.kt        <- Panel de control, métricas y recordatorios
│       │   │       │   ├── ClientsScreen.kt          <- Directorio de clientes y vehículos asociados
│       │   │       │   ├── AddEditClientDialog.kt    <- Diálogo de alta y edición de cliente
│       │   │       │   ├── AddEditVehicleDialog.kt   <- Diálogo de alta y edición de vehículo
│       │   │       │   ├── NewServiceScreen.kt       <- Formulario técnico de servicio y programación
│       │   │       │   ├── ServiceHistoryScreen.kt   <- Historial de órdenes de servicio y tickets
│       │   │       │   └── BackupSettingsScreen.kt   <- Configuración de Google Sheets y Push
│       │   │       └── theme/
│       │   │           ├── Color.kt                  <- Paleta automotriz (Azul técnico, Ámbar, Acero)
│       │   │           ├── Theme.kt                  <- Tema Material 3 dinámico
│       │   │           └── Type.kt                   <- Tipografía
│       │   └── res/
│       │       ├── values/strings.xml
│       │       ├── xml/file_paths.xml                <- Rutas de FileProvider para compartir CSV
│       │       └── mipmap-*/                         <- Ícono adaptativo personalizado automotriz
```

---

## ☁️ Configuración del Respaldo en Google Sheets (Paso a Paso)

Para activar el respaldo automático en tiempo real en tu propia hoja de cálculo de Google:

1. Crea una nueva hoja de cálculo en [Google Sheets](https://sheets.new).
2. En el menú superior de la hoja, haz clic en **Extensiones > Apps Script**.
3. Borra el código predeterminado y pega el siguiente script:

```javascript
/**
 * GOOGLE APPS SCRIPT PARA RESPALDO AUTOMÁTICO DE AUTOMANTENIMIENTO
 */
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    
    // 1. Hoja de Historial de Servicios
    var sheetServices = ss.getSheetByName("Historial_Servicios");
    if (!sheetServices) {
      sheetServices = ss.insertSheet("Historial_Servicios");
      sheetServices.appendRow([
        "ID", "Fecha", "Cliente", "Placa", "Vehículo", "Km Servicio", 
        "Tipo Servicio", "Aceite Utilizado", "Repuestos", "Total $", "Próximo Km", "Próxima Fecha"
      ]);
      sheetServices.getRange(1, 1, 1, 12).setFontWeight("bold").setBackground("#e0e0e0");
    }
    
    if (data.services && data.services.length > 0) {
      data.services.forEach(function(s) {
        sheetServices.appendRow([
          s.id, s.serviceDate, s.clientName, s.vehiclePlate, s.vehicleModel,
          s.mileageAtService, s.serviceType, s.oilDetails, s.partsDescription,
          s.totalCost, s.nextServiceMileage, s.nextServiceDate
        ]);
      });
    }

    // 2. Hoja de Clientes
    var sheetClients = ss.getSheetByName("Clientes");
    if (!sheetClients) {
      sheetClients = ss.insertSheet("Clientes");
      sheetClients.appendRow(["ID", "Nombre", "Teléfono", "Email", "Cédula", "Dirección"]);
      sheetClients.getRange(1, 1, 1, 6).setFontWeight("bold").setBackground("#d0e0fc");
    }
    if (data.clients && data.clients.length > 0) {
      data.clients.forEach(function(c) {
        sheetClients.appendRow([c.id, c.name, c.phone, c.email, c.documentId, c.address]);
      });
    }
    
    return ContentService.createTextOutput(JSON.stringify({status: "success", count: data.services ? data.services.length : 0}))
      .setMimeType(ContentService.MimeType.JSON);
  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: error.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
```

4. Haz clic en **Implementar > Nueva implementación**.
5. Selecciona el tipo: **Aplicación web**.
6. En **Quién tiene acceso**, selecciona: **Cualquier persona** (*Anyone*).
7. Haz clic en **Implementar** y copia la **URL de la aplicación web** generada (termina en `/exec`).
8. Abre la app en tu celular, ve a la pestaña **Respaldo**, pega la URL y pulsa **Guardar URL**.
9. Pulsa **Sincronizar Ya** para verificar la conexión.

---

## 🚀 Cómo Subir tu Proyecto a GitHub

Sigue estos comandos para publicar y versionar tu repositorio:

```bash
# 1. Abre tu terminal en la raíz del proyecto
cd ruta/del/proyecto

# 2. Inicializa el repositorio Git (si no está inicializado)
git init

# 3. Agrega todos los archivos al control de versiones
git add .

# 4. Crea el primer commit descriptivo
git commit -m "feat: Lanzamiento inicial de AutoMantenimiento Pro con Room, Sheets y WhatsApp"

# 5. Cambia a la rama principal
git branch -M main

# 6. Agrega el origen remoto de tu repositorio en GitHub
git remote add origin https://github.com/TU_USUARIO/TU_REPOSITORIO.git

# 7. Sube el código a GitHub
git push -u origin main
```

---

## 📱 Compilación y Generación del APK

Para compilar la aplicación y generar el archivo APK instalable en cualquier dispositivo Android:

```bash
# Compilar versión de depuración
gradle :app:assembleDebug

# El APK generado se encontrará en:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛡️ Seguridad y Buenas Prácticas
- Toda la base de datos se almacena localmente de forma privada en el almacenamiento interno de la app vía SQLite / Room.
- La exportación a CSV utiliza un `FileProvider` seguro sin requerir permisos de almacenamiento externos peligrosos.
- Las notificaciones cumplen con las directrices de Android 13+ con solicitud en tiempo de ejecución.
