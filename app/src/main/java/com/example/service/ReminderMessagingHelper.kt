package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.AlertStatus
import com.example.data.model.MaintenanceAlert
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderMessagingHelper {

    fun generateMessage(alert: MaintenanceAlert, workshopName: String = "Taller AutoMantenimiento"): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val targetDateStr = if (alert.targetDate > 0L) dateFormat.format(Date(alert.targetDate)) else "Próximamente"

        val statusText = when (alert.status) {
            AlertStatus.OVERDUE -> "⚠️ AVISO URGENTE: El servicio se encuentra vencido."
            AlertStatus.UPCOMING -> "🔔 RECORDATORIO: El servicio está próximo a vencer."
            AlertStatus.UP_TO_DATE -> "✅ Mantenimiento programado al día."
        }

        return buildString {
            append("Estimado(a) *${alert.client.name}*,\n\n")
            append("Le saludamos cordialmente de *$workshopName*.\n\n")
            append("$statusText\n\n")
            append("📋 *Detalles del Vehículo:*\n")
            append("• *Vehículo:* ${alert.vehicle.brand} ${alert.vehicle.model} (${alert.vehicle.year})\n")
            append("• *Placa:* ${alert.vehicle.plateNumber}\n")
            append("• *Servicio Programado:* ${alert.serviceType}\n")
            if (alert.targetMileage > 0) {
                append("• *Kilometraje Objetivo:* ${alert.targetMileage} km (Actual: ${alert.currentMileage} km)\n")
            }
            if (alert.targetDate > 0L) {
                append("• *Fecha estimada:* $targetDateStr\n")
            }
            append("\n")
            append("El cambio oportuno de fluidos y repuestos garantiza la vida útil y seguridad de su motor.\n\n")
            append("¿Desea agendar su cita para esta semana? Responda a este mensaje para confirmar día y hora.\n\n")
            append("¡Quedamos atentos a sus órdenes!\n")
            append("*$workshopName*")
        }
    }

    fun sendViaWhatsApp(context: Context, alert: MaintenanceAlert, workshopName: String = "Taller AutoMantenimiento") {
        try {
            val message = generateMessage(alert, workshopName)
            val cleanPhone = alert.client.phone.replace(Regex("[^0-9+]"), "").replace("+", "")

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No se pudo abrir WhatsApp. Verifique que esté instalado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun sendViaEmail(context: Context, alert: MaintenanceAlert, workshopName: String = "Taller AutoMantenimiento") {
        try {
            val subject = "Recordatorio de Mantenimiento - ${alert.vehicle.brand} ${alert.vehicle.model} (${alert.vehicle.plateNumber})"
            val message = generateMessage(alert, workshopName).replace("*", "") // Plain text formatting

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                if (alert.client.email.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(alert.client.email))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Direct fallback
                context.startActivity(Intent.createChooser(intent, "Enviar correo recordatorio"))
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No se encontró una aplicación de correo electrónico configurada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
