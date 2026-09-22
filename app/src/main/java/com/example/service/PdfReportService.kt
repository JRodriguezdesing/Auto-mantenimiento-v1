package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Client
import com.example.data.model.MaintenanceQuote
import com.example.data.model.ServiceRecord
import com.example.data.model.Vehicle
import com.example.data.model.VehicleWithClient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportService {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Standard A4 dimensions in points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Safely share a PDF file using FileProvider and Android Chooser
     */
    fun sharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Generates a single Service Order / Maintenance Ticket PDF with full workshop colors and layout
     */
    fun generateServiceTicketPdf(
        context: Context,
        client: Client,
        vehicle: Vehicle,
        service: ServiceRecord
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawServiceTicketCanvas(canvas, client, vehicle, service)

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(cacheDir, "mantenimiento_${vehicle.plateNumber}_orden${service.id}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawServiceTicketCanvas(
        canvas: Canvas,
        client: Client,
        vehicle: Vehicle,
        service: ServiceRecord
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner (Dark Navy Blue #1E3A8A)
        paint.color = Color.parseColor("#1E3A8A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Accent amber stripe
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 95f, PAGE_WIDTH.toFloat(), 100f, paint)

        // Header Title: AUTO MANTENIMIENTO
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("AUTO MANTENIMIENTO", 30f, 42f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("TALLER MECÁNICO Y ELECTROMECÁNICO ESPECIALIZADO", 30f, 60f, paint)
        canvas.drawText("Control Técnico • Lubricación • Diagnóstico Computarizado", 30f, 75f, paint)

        // Header Right: Order # and Date
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 13f
        paint.isFakeBoldText = true
        paint.color = Color.WHITE
        canvas.drawText("ORDEN N° #${service.id}", (PAGE_WIDTH - 30).toFloat(), 42f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawText("Fecha: ${dateFormat.format(Date(service.serviceDate))}", (PAGE_WIDTH - 30).toFloat(), 60f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 2. Client & Vehicle Information Cards
        var currentY = 120f

        // Vehicle Box
        val boxWidth = (PAGE_WIDTH - 70) / 2f
        val boxHeight = 115f

        // Box Vehicle
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        val vehRect = RectF(30f, currentY, 30f + boxWidth, currentY + boxHeight)
        canvas.drawRoundRect(vehRect, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(vehRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Header inside Box Vehicle
        paint.color = Color.parseColor("#3B82F6")
        canvas.drawRect(30f, currentY, 30f + boxWidth, currentY + 24f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("DATOS DEL VEHÍCULO", 40f, currentY + 16f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val vy = currentY + 38f
        canvas.drawText("Placa: ", 40f, vy, paint)
        paint.isFakeBoldText = true
        canvas.drawText(vehicle.plateNumber, 85f, vy, paint)
        paint.isFakeBoldText = false
        canvas.drawText("Vehículo: ${vehicle.brand} ${vehicle.model} (${vehicle.year})", 40f, vy + 18f, paint)
        canvas.drawText("Kilometraje Ingreso: ${service.mileageAtService} km", 40f, vy + 36f, paint)
        canvas.drawText("Combustible: ${vehicle.engineType}", 40f, vy + 54f, paint)

        // Box Client
        val clientX = 40f + boxWidth
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        val cliRect = RectF(clientX, currentY, clientX + boxWidth, currentY + boxHeight)
        canvas.drawRoundRect(cliRect, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(cliRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Header inside Box Client
        paint.color = Color.parseColor("#0F766E")
        canvas.drawRect(clientX, currentY, clientX + boxWidth, currentY + 24f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("DATOS DEL CLIENTE", clientX + 10f, currentY + 16f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val cy = currentY + 38f
        canvas.drawText("Nombre: ", clientX + 10f, cy, paint)
        paint.isFakeBoldText = true
        canvas.drawText(client.name, clientX + 60f, cy, paint)
        paint.isFakeBoldText = false
        canvas.drawText("Teléfono / WhatsApp: ${client.phone}", clientX + 10f, cy + 18f, paint)
        canvas.drawText("Correo: ${client.email.ifBlank { "N/A" }}", clientX + 10f, cy + 36f, paint)
        canvas.drawText("Identificación: ${client.documentId.ifBlank { "N/A" }}", clientX + 10f, cy + 54f, paint)

        // 3. Technical Service Work Details Table
        currentY += boxHeight + 20f

        paint.color = Color.parseColor("#1E3A8A")
        paint.style = Paint.Style.FILL
        val tableHeaderRect = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 24f)
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("DESCRIPCIÓN DE TRABAJOS Y MANTENIMIENTOS EFECTUADOS", 40f, currentY + 16f, paint)

        currentY += 28f
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.isFakeBoldText = false

        // Work Types
        val serviceLines = service.serviceType.split("+").map { it.trim() }
        serviceLines.forEach { line ->
            paint.isFakeBoldText = true
            paint.color = Color.parseColor("#2563EB")
            canvas.drawText("✔", 40f, currentY + 10f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.isFakeBoldText = false
            canvas.drawText(line, 55f, currentY + 10f, paint)
            currentY += 16f
        }

        currentY += 10f

        // 4. Lubrication & Parts Table
        val partsTableRect = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 22f)
        paint.color = Color.parseColor("#475569")
        canvas.drawRoundRect(partsTableRect, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("DETALLE DE LUBRICACIÓN Y REPUESTOS", 40f, currentY + 15f, paint)
        canvas.drawText("COSTO", (PAGE_WIDTH - 85).toFloat(), currentY + 15f, paint)

        currentY += 24f
        paint.color = Color.parseColor("#1E293B")
        paint.isFakeBoldText = false

        // Oil info
        paint.isFakeBoldText = true
        canvas.drawText("Aceite / Fluidos:", 40f, currentY + 12f, paint)
        paint.isFakeBoldText = false
        canvas.drawText(service.oilDetails.ifBlank { "N/A" }, 140f, currentY + 12f, paint)
        currentY += 18f

        // Replaced Parts
        paint.isFakeBoldText = true
        canvas.drawText("Repuestos Reemplazados:", 40f, currentY + 12f, paint)
        paint.isFakeBoldText = false
        val partsText = service.partsDescription.ifBlank { "Ninguno" }
        canvas.drawText(partsText, 180f, currentY + 12f, paint)
        currentY += 24f

        // 5. Cost Summary Table
        val summaryX = PAGE_WIDTH - 230f
        paint.color = Color.parseColor("#F1F5F9")
        val summaryRect = RectF(summaryX, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 75f)
        canvas.drawRoundRect(summaryRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(summaryRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas.drawText("Costo de Repuestos:", summaryX + 12f, currentY + 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", service.partsCost)}", (PAGE_WIDTH - 42).toFloat(), currentY + 18f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Mano de Obra Calificada:", summaryX + 12f, currentY + 36f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", service.laborCost)}", (PAGE_WIDTH - 42).toFloat(), currentY + 36f, paint)

        paint.color = Color.parseColor("#1E3A8A")
        paint.isFakeBoldText = true
        paint.textSize = 12f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL FACTURADO:", summaryX + 12f, currentY + 62f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", service.totalCost)}", (PAGE_WIDTH - 42).toFloat(), currentY + 62f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 6. Next Service Interval Banner
        currentY += 88f
        val nextBannerRect = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 60f)
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(nextBannerRect, 8f, 8f, paint)
        paint.color = Color.parseColor("#F59E0B")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(nextBannerRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#B45309")
        paint.isFakeBoldText = true
        paint.textSize = 11f
        canvas.drawText("🎯 PROGRAMACIÓN DE PRÓXIMO MANTENIMIENTO TÉCNICO", 45f, currentY + 22f, paint)
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val nextDateStr = if (service.nextServiceDate > 0) dateOnlyFormat.format(Date(service.nextServiceDate)) else "Según desgaste"
        canvas.drawText("• Kilometraje meta para próximo servicio: ${service.nextServiceMileage} km", 45f, currentY + 38f, paint)
        canvas.drawText("• Fecha sugerida para próxima revisión: $nextDateStr", 45f, currentY + 52f, paint)

        // 7. Notes and Observations
        if (service.notes.isNotBlank()) {
            currentY += 72f
            paint.color = Color.parseColor("#334155")
            paint.isFakeBoldText = true
            paint.textSize = 9.5f
            canvas.drawText("Observaciones y Recomendaciones:", 30f, currentY, paint)
            paint.isFakeBoldText = false
            currentY += 14f
            canvas.drawText(service.notes, 30f, currentY, paint)
        }

        // 8. Footer & Signature
        val footerY = PAGE_HEIGHT - 60f
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(50f, footerY, 220f, footerY, paint)
        canvas.drawLine((PAGE_WIDTH - 220).toFloat(), footerY, (PAGE_WIDTH - 50).toFloat(), footerY, paint)

        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Firma / Sello del Taller", 135f, footerY + 14f, paint)
        canvas.drawText("Firma del Cliente (Conforme)", (PAGE_WIDTH - 135).toFloat(), footerY + 14f, paint)

        paint.textSize = 8f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("AUTO MANTENIMIENTO • Documento generado digitalmente • Garantía de Servicio Profesional", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 20).toFloat(), paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /**
     * Generates a Quote / Proforma PDF (Cotización de Materiales y Mano de Obra)
     */
    fun generateQuotePdf(context: Context, quote: MaintenanceQuote): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawQuoteCanvas(canvas, quote)

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(cacheDir, "cotizacion_${quote.vehiclePlate}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawQuoteCanvas(canvas: Canvas, quote: MaintenanceQuote) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Banner (Teal / Navy Blue Gradient Style)
        paint.color = Color.parseColor("#0F172A") // Deep slate
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 100f, paint)

        paint.color = Color.parseColor("#0284C7") // Sky blue accent line
        canvas.drawRect(0f, 100f, PAGE_WIDTH.toFloat(), 105f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("AUTO MANTENIMIENTO", 30f, 42f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#38BDF8")
        canvas.drawText("COTIZACIÓN DE MATERIALES Y MANO DE OBRA", 30f, 62f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("Presupuesto detallado para autorización del cliente", 30f, 78f, paint)

        // Right side: Quote # and Date
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText(quote.quoteNumber.ifBlank { "COTIZACIÓN N° #" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(quote.date)) }, (PAGE_WIDTH - 30).toFloat(), 42f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawText("Fecha: ${dateOnlyFormat.format(Date(quote.date))}", (PAGE_WIDTH - 30).toFloat(), 62f, paint)
        canvas.drawText("Validez: ${quote.validityDays} días", (PAGE_WIDTH - 30).toFloat(), 78f, paint)
        paint.textAlign = Paint.Align.LEFT

        var currentY = 125f

        // Info Block (Client & Vehicle)
        val infoRect = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 68f)
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(infoRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(infoRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 10f
        // Col 1: Cliente
        paint.isFakeBoldText = true
        canvas.drawText("Cliente:", 45f, currentY + 22f, paint)
        paint.isFakeBoldText = false
        canvas.drawText(quote.clientName.ifBlank { "Cliente General" }, 95f, currentY + 22f, paint)
        canvas.drawText("Teléfono / WhatsApp: ${quote.clientPhone.ifBlank { "N/A" }}", 45f, currentY + 44f, paint)

        // Col 2: Vehículo
        val col2X = PAGE_WIDTH / 2f + 10f
        paint.isFakeBoldText = true
        canvas.drawText("Vehículo:", col2X, currentY + 22f, paint)
        paint.isFakeBoldText = false
        canvas.drawText("${quote.vehiclePlate} • ${quote.vehicleModel}", col2X + 55f, currentY + 22f, paint)
        canvas.drawText("Kilometraje actual: ${if (quote.currentMileage > 0) "${quote.currentMileage} km" else "No registrado"}", col2X, currentY + 44f, paint)

        currentY += 85f

        // Materials Table Header
        paint.color = Color.parseColor("#0284C7")
        val tableHead = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 24f)
        canvas.drawRoundRect(tableHead, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("ÍTEM / MATERIAL / REPUESTO AUTORIZADO", 45f, currentY + 16f, paint)
        canvas.drawText("CANT.", (PAGE_WIDTH - 180).toFloat(), currentY + 16f, paint)
        canvas.drawText("P. UNIT.", (PAGE_WIDTH - 120).toFloat(), currentY + 16f, paint)
        canvas.drawText("SUBTOTAL", (PAGE_WIDTH - 65).toFloat(), currentY + 16f, paint)

        currentY += 26f

        // Material rows
        paint.textSize = 9.5f
        if (quote.materials.isEmpty()) {
            paint.color = Color.parseColor("#64748B")
            paint.isFakeBoldText = false
            canvas.drawText("No se especificaron repuestos (solo mano de obra)", 45f, currentY + 14f, paint)
            currentY += 24f
        } else {
            var isAlt = false
            quote.materials.forEachIndexed { index, item ->
                if (isAlt) {
                    paint.color = Color.parseColor("#F1F5F9")
                    canvas.drawRect(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 20f, paint)
                }
                isAlt = !isAlt

                paint.color = Color.parseColor("#1E293B")
                paint.isFakeBoldText = false
                canvas.drawText("${index + 1}. ${item.name}", 45f, currentY + 14f, paint)

                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                canvas.drawText(qtyStr, (PAGE_WIDTH - 165).toFloat(), currentY + 14f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("$ ${String.format(Locale.US, "%.2f", item.unitPrice)}", (PAGE_WIDTH - 90).toFloat(), currentY + 14f, paint)
                canvas.drawText("$ ${String.format(Locale.US, "%.2f", item.subtotal)}", (PAGE_WIDTH - 35).toFloat(), currentY + 14f, paint)
                paint.textAlign = Paint.Align.LEFT

                currentY += 20f
            }
        }

        // Labor Section
        currentY += 10f
        paint.color = Color.parseColor("#334155")
        val laborHead = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 22f)
        canvas.drawRoundRect(laborHead, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.isFakeBoldText = true
        canvas.drawText("MANO DE OBRA TÉCNICA (${quote.laborRole.uppercase(Locale.getDefault())})", 45f, currentY + 15f, paint)
        canvas.drawText("VALOR", (PAGE_WIDTH - 65).toFloat(), currentY + 15f, paint)

        currentY += 24f
        paint.color = Color.parseColor("#1E293B")
        paint.isFakeBoldText = false
        canvas.drawText(quote.laborDescription.ifBlank { "Mano de obra y calibración" }, 45f, currentY + 14f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.isFakeBoldText = true
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", quote.laborCost)}", (PAGE_WIDTH - 35).toFloat(), currentY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Totals Box
        currentY += 30f
        val totalBoxX = PAGE_WIDTH - 240f
        val totalBoxRect = RectF(totalBoxX, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 70f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(totalBoxRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(totalBoxRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 9.5f
        paint.color = Color.parseColor("#475569")
        canvas.drawText("Subtotal Materiales:", totalBoxX + 12f, currentY + 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", quote.materialsTotal)}", (PAGE_WIDTH - 42).toFloat(), currentY + 18f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Mano de Obra (${quote.laborRole}):", totalBoxX + 12f, currentY + 36f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", quote.laborCost)}", (PAGE_WIDTH - 42).toFloat(), currentY + 36f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL ESTIMADO:", totalBoxX + 12f, currentY + 60f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.parseColor("#0284C7")
        canvas.drawText("$ ${String.format(Locale.US, "%.2f", quote.grandTotal)}", (PAGE_WIDTH - 42).toFloat(), currentY + 60f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Notes and Disclaimer
        currentY += 85f
        val notesRect = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 45f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(notesRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("Términos de la Cotización:", 40f, currentY + 16f, paint)
        paint.isFakeBoldText = false
        canvas.drawText(quote.notes, 40f, currentY + 32f, paint)

        // Signatures
        val footerY = PAGE_HEIGHT - 65f
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 1f
        canvas.drawLine(50f, footerY, 220f, footerY, paint)
        canvas.drawLine((PAGE_WIDTH - 220).toFloat(), footerY, (PAGE_WIDTH - 50).toFloat(), footerY, paint)

        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Aprobado por el Taller", 135f, footerY + 14f, paint)
        canvas.drawText("Autorización del Cliente", (PAGE_WIDTH - 135).toFloat(), footerY + 14f, paint)

        paint.textSize = 8f
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("AUTO MANTENIMIENTO • Cotización válida por ${quote.validityDays} días • Precios en Dólares ($)", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 20).toFloat(), paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /**
     * Generates a Full Report of all Service History records in PDF
     */
    fun generateAllServicesSummaryPdf(
        context: Context,
        services: List<ServiceRecord>,
        vehicles: List<Vehicle>,
        clients: List<Client>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val clientMap = clients.associateBy { it.id }
        val vehicleMap = vehicles.associateBy { it.id }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = Color.parseColor("#1E3A8A")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 85f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("AUTO MANTENIMIENTO", 30f, 40f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawText("INFORME CONSOLIDADO DE SERVICIOS TÉCNICOS", 30f, 58f, paint)
        canvas.drawText("Total Servicios: ${services.size} | Facturación: $ ${String.format(Locale.US, "%.2f", services.sumOf { it.totalCost })}", 30f, 72f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generado: ${dateFormat.format(Date())}", (PAGE_WIDTH - 30).toFloat(), 58f, paint)
        paint.textAlign = Paint.Align.LEFT

        var currentY = 110f

        // Table Header
        paint.color = Color.parseColor("#0F172A")
        val tableHead = RectF(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 22f)
        canvas.drawRoundRect(tableHead, 4f, 4f, paint)
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("FECHA", 35f, currentY + 15f, paint)
        canvas.drawText("PLACA", 100f, currentY + 15f, paint)
        canvas.drawText("CLIENTE", 160f, currentY + 15f, paint)
        canvas.drawText("TRABAJOS REALIZADOS", 260f, currentY + 15f, paint)
        canvas.drawText("KM", 430f, currentY + 15f, paint)
        canvas.drawText("TOTAL ($)", (PAGE_WIDTH - 75).toFloat(), currentY + 15f, paint)

        currentY += 24f

        var isAlt = false
        val displayServices = services.take(30) // Render up to 30 most recent per sheet
        displayServices.forEach { s ->
            val v = vehicleMap[s.vehicleId]
            val c = clientMap[s.clientId]

            if (isAlt) {
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRect(30f, currentY, (PAGE_WIDTH - 30).toFloat(), currentY + 18f, paint)
            }
            isAlt = !isAlt

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText(dateOnlyFormat.format(Date(s.serviceDate)), 35f, currentY + 12f, paint)
            paint.isFakeBoldText = true
            canvas.drawText(v?.plateNumber ?: "N/A", 100f, currentY + 12f, paint)
            paint.isFakeBoldText = false
            canvas.drawText((c?.name ?: "").take(16), 160f, currentY + 12f, paint)
            canvas.drawText(s.serviceType.take(28), 260f, currentY + 12f, paint)
            canvas.drawText("${s.mileageAtService}", 430f, currentY + 12f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.isFakeBoldText = true
            canvas.drawText("$ ${String.format(Locale.US, "%.2f", s.totalCost)}", (PAGE_WIDTH - 35).toFloat(), currentY + 12f, paint)
            paint.textAlign = Paint.Align.LEFT

            currentY += 18f
        }

        // Summary Line at bottom
        currentY += 15f
        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Total Facturación Acumulada (${services.size} servicios): $ ${String.format(Locale.US, "%.2f", services.sumOf { it.totalCost })}", 35f, currentY, paint)

        pdfDocument.finishPage(page)

        return try {
            val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(cacheDir, "informe_general_mantenimientos_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
