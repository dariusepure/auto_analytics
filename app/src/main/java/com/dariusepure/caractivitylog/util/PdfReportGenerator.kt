package com.dariusepure.caractivitylog.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.dariusepure.caractivitylog.domain.Car
import com.dariusepure.caractivitylog.domain.FuelLog
import com.dariusepure.caractivitylog.domain.MileageLog
import com.dariusepure.caractivitylog.domain.VehicleInspection
import com.dariusepure.caractivitylog.domain.displayName
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfReportGenerator {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun generateReport(
        context: Context,
        car: Car,
        mileageLogs: List<MileageLog>,
        inspections: List<VehicleInspection>,
        fuelLogs: List<FuelLog>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        var yPosition = 40f

        // Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Vehicle History Report", 40f, yPosition, paint)
        yPosition += 40f

        // Car Details
        paint.textSize = 18f
        canvas.drawText(car.displayName, 40f, yPosition, paint)
        yPosition += 25f
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("License Plate: ${car.licensePlate}", 40f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("VIN: ${car.vin}", 40f, yPosition, paint)
        yPosition += 20f
        canvas.drawText("Year: ${car.year}", 40f, yPosition, paint)
        yPosition += 40f

        // Mileage History
        if (mileageLogs.isNotEmpty()) {
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Mileage History", 40f, yPosition, paint)
            yPosition += 25f
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            mileageLogs.sortedByDescending { it.date }.take(10).forEach { log ->
                canvas.drawText("${dateFormat.format(log.date)}: ${log.km.toInt()} km", 60f, yPosition, paint)
                yPosition += 20f
                if (yPosition > 800) return@forEach // Basic overflow check
            }
            yPosition += 20f
        }

        // Inspections
        if (inspections.isNotEmpty()) {
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Vehicle Inspections (ITP)", 40f, yPosition, paint)
            yPosition += 25f
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            inspections.sortedByDescending { it.date }.forEach { inspection ->
                canvas.drawText("${dateFormat.format(inspection.date)} - Mileage: ${inspection.mileage.toInt()} km", 60f, yPosition, paint)
                yPosition += 20f
                canvas.drawText("  Valid until: ${dateFormat.format(inspection.expiryDate)}", 60f, yPosition, paint)
                yPosition += 20f
                if (yPosition > 800) return@forEach
            }
            yPosition += 20f
        }

        // Fuel Logs
        if (fuelLogs.isNotEmpty()) {
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Fuel Consumption", 40f, yPosition, paint)
            yPosition += 25f
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            fuelLogs.sortedByDescending { it.date }.take(10).forEach { log ->
                canvas.drawText("${dateFormat.format(log.date)}: ${log.liters}L at ${log.km.toInt()} km", 60f, yPosition, paint)
                yPosition += 20f
                if (yPosition > 800) return@forEach
            }
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
